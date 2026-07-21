package com.yusufteker.pulse.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/** Renk ve ikon listesi. index = avatarId sayısı - 1 */
private val avatarColors = listOf(
    0xFF6C5CE7, // 1 - mor
    0xFFE63946, // 2 - kırmızı
    0xFF2A9D8F, // 3 - teal
    0xFF9D4EDD, // 4 - koyu mor
    0xFFE83E8C, // 5 - pembe
    0xFFF4A261, // 6 - turuncu
    0xFF00B4D8, // 7 - açık mavi
    0xFF3F37C9, // 8 - lacivert
    0xFFFFB703, // 9 - sarı
    0xFF2D6A4F, // 10 - koyu yeşil
)

private val avatarIcons: List<ImageVector> = listOf(
    Icons.Rounded.Person,
    Icons.Rounded.Face,
    Icons.Rounded.Star,
    Icons.Rounded.Favorite,
    Icons.Rounded.FlashOn,
    Icons.Rounded.Pets,
    Icons.Rounded.EmojiNature,
    Icons.Rounded.SelfImprovement,
    Icons.Rounded.Rocket,
    Icons.Rounded.AutoAwesome,
)

/**
 * Native Compose avatar: renkli daire + Material ikon veya Coil ile URL'den resim yükleme.
 * XML Vector Drawable kullanmaz, hiçbir paketleme sorunu yaşatmaz.
 */
fun getOptimizedCloudinaryUrl(url: String): String {
    if (!url.contains("res.cloudinary.com") || !url.contains("/upload/")) return url
    // Eğer halihazırda transformasyon eklenmişse tekrar ekleme
    if (url.contains("/upload/w_")) return url 
    return url.replaceFirst("/upload/", "/upload/w_200,h_200,c_fill,q_auto,f_auto/")
}

@Composable
fun AvatarImage(avatarId: String? = null, modifier: Modifier = Modifier, profileImageUrl: String? = null) {
    if (profileImageUrl != null) {
        val optimizedUrl = getOptimizedCloudinaryUrl(profileImageUrl)
        AsyncImage(
            model = optimizedUrl,
            contentDescription = "Profile Image",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val index = ((avatarId ?: "").removePrefix("avatar_").toIntOrNull() ?: 1) - 1
        val colorHex = avatarColors.getOrElse(index) { avatarColors[0] }
        val icon = avatarIcons.getOrElse(index) { avatarIcons[0] }

        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(Color(colorHex)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    }
}
