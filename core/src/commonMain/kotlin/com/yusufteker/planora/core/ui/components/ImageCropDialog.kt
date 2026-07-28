package com.yusufteker.planora.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.yusufteker.planora.core.utils.CropRectNormalized
import com.yusufteker.planora.core.utils.cropImageBytes
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.cancel
import planora.core.generated.resources.crop_confirm
import planora.core.generated.resources.crop_image_instructions
import planora.core.generated.resources.crop_image_title

/**
 * Interactive Cross-Platform Image Crop Dialog for Kotlin Multiplatform.
 * Displays the complete original image without pre-cropping (using ContentScale.Fit),
 * allowing users to freely drag and zoom across the entire image.
 *
 * @param imageBytes The raw image bytes selected by the user
 * @param isCircular If true, displays a circular mask (e.g. for user avatar), otherwise square
 * @param onImageCropped Callback receiving the cropped image byte array
 * @param onDismiss Callback invoked when the user cancels the dialog
 */
@Composable
fun ImageCropDialog(
    imageBytes: ByteArray,
    isCircular: Boolean = true,
    onImageCropped: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageSize by remember { mutableStateOf<IntSize?>(null) }

    val density = LocalDensity.current
    val cropPaddingPx = with(density) { 16.dp.toPx() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.crop_image_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(Res.string.crop_image_instructions),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Crop Viewport Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clipToBounds()
                            .background(Color.Black)
                            .onGloballyPositioned { coordinates ->
                                containerSize = coordinates.size
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    val maxOffset = containerSize.width.toFloat() * scale
                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                                        y = (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Original image displayed in full aspect ratio without pre-cropping
                        AsyncImage(
                            model = imageBytes,
                            contentDescription = "Crop Preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                            contentScale = ContentScale.Fit,
                            onSuccess = { state ->
                                val painter = state.painter
                                val intrinsic = painter.intrinsicSize
                                if (intrinsic.width > 0 && intrinsic.height > 0) {
                                    imageSize = IntSize(intrinsic.width.toInt(), intrinsic.height.toInt())
                                }
                            }
                        )

                        // Mask Overlay (Hole Punch for Crop Window)
                        val primaryColor = MaterialTheme.colorScheme.primary
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val side = size.minDimension
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val cropRadius = side / 2f - cropPaddingPx

                            val path = Path().apply {
                                addRect(Rect(Offset.Zero, size))
                                if (isCircular) {
                                    addOval(Rect(center, cropRadius))
                                } else {
                                    val rectSide = cropRadius * 2f
                                    addRoundRect(
                                        androidx.compose.ui.geometry.RoundRect(
                                            rect = Rect(
                                                left = center.x - cropRadius,
                                                top = center.y - cropRadius,
                                                right = center.x + cropRadius,
                                                bottom = center.y + cropRadius
                                            ),
                                            cornerRadius = CornerRadius(16.dp.toPx())
                                        )
                                    )
                                }
                                fillType = PathFillType.EvenOdd
                            }

                            // Dimmed background surrounding crop window
                            drawPath(
                                path = path,
                                color = Color.Black.copy(alpha = 0.65f)
                            )

                            // Highlight border around crop window
                            if (isCircular) {
                                drawCircle(
                                    color = primaryColor,
                                    radius = cropRadius,
                                    center = center,
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            } else {
                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(center.x - cropRadius, center.y - cropRadius),
                                    size = Size(cropRadius * 2f, cropRadius * 2f),
                                    cornerRadius = CornerRadius(16.dp.toPx()),
                                    style = Stroke(width = 3.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Zoom Slider Control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Slider(
                            value = scale,
                            onValueChange = { newScale ->
                                scale = newScale
                                val maxOffset = containerSize.width.toFloat() * scale
                                offset = Offset(
                                    x = offset.x.coerceIn(-maxOffset, maxOffset),
                                    y = offset.y.coerceIn(-maxOffset, maxOffset)
                                )
                            },
                            valueRange = 1f..4f,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        Text(
                            text = "4x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dialog Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = stringResource(Res.string.cancel))
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                val normalizedCrop = calculateNormalizedCrop(
                                    scale = scale,
                                    offset = offset,
                                    containerSize = containerSize,
                                    imageSize = imageSize,
                                    cropPaddingPx = cropPaddingPx
                                )
                                val croppedBytes = cropImageBytes(imageBytes, normalizedCrop)
                                onImageCropped(croppedBytes)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = stringResource(Res.string.crop_confirm),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calculates normalized coordinates (0.0 to 1.0) of the crop region on the original image,
 * taking into account ContentScale.Fit positioning and user scale/offset transformations.
 */
private fun calculateNormalizedCrop(
    scale: Float,
    offset: Offset,
    containerSize: IntSize,
    imageSize: IntSize?,
    cropPaddingPx: Float
): CropRectNormalized {
    if (containerSize.width == 0 || containerSize.height == 0) {
        return CropRectNormalized(0f, 0f, 1f, 1f)
    }

    val W = containerSize.width.toFloat()
    val H = containerSize.height.toFloat()

    val imgW = imageSize?.width?.toFloat() ?: W
    val imgH = imageSize?.height?.toFloat() ?: H

    // Aspect ratio fit dimensions inside container at scale=1
    val scaleFit = minOf(W / imgW, H / imgH)
    val drawnW = imgW * scaleFit
    val drawnH = imgH * scaleFit

    val drawnLeft = (W - drawnW) / 2f
    val drawnTop = (H - drawnH) / 2f

    // Window bounds in container space
    val side = minOf(W, H)
    val cropRadius = side / 2f - cropPaddingPx

    val windowLeft = W / 2f - cropRadius
    val windowRight = W / 2f + cropRadius
    val windowTop = H / 2f - cropRadius
    val windowBottom = H / 2f + cropRadius

    // Inverse transform container crop window back to drawn image coordinates
    val xImgLeft = (windowLeft - W / 2f - offset.x) / scale + W / 2f - drawnLeft
    val xImgRight = (windowRight - W / 2f - offset.x) / scale + W / 2f - drawnLeft
    val yImgTop = (windowTop - H / 2f - offset.y) / scale + H / 2f - drawnTop
    val yImgBottom = (windowBottom - H / 2f - offset.y) / scale + H / 2f - drawnTop

    val normLeft = (xImgLeft / drawnW).coerceIn(0f, 1f)
    val normTop = (yImgTop / drawnH).coerceIn(0f, 1f)
    val normRight = (xImgRight / drawnW).coerceIn(0f, 1f)
    val normBottom = (yImgBottom / drawnH).coerceIn(0f, 1f)

    val normWidth = (normRight - normLeft).coerceIn(0.01f, 1f)
    val normHeight = (normBottom - normTop).coerceIn(0.01f, 1f)

    return CropRectNormalized(
        left = normLeft,
        top = normTop,
        width = normWidth,
        height = normHeight
    )
}
