package com.yusufteker.planora.feature.home.presentation.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import com.yusufteker.planora.feature.home.presentation.util.PlanoraIcons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.absoluteValue
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.feature.home.domain.model.Post
import com.yusufteker.planora.feature.home.domain.model.Topic
import com.yusufteker.planora.feature.home.presentation.util.color
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import androidx.compose.material.icons.rounded.Share
import com.mikepenz.markdown.m3.Markdown

@Composable
fun PostCard(
    post: Post,
    onClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Sol Taraf: Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(generateAvatarColor(post.authorUsername))
            ) {
                Text(
                    text = post.authorName.take(1).uppercase(),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Sağ Taraf: İçerik
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Başlık Satırı: İsim, @KullanıcıAdı, Tarih
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "@${post.authorUsername}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val date = kotlinx.datetime.Instant.fromEpochMilliseconds(post.createdAt)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    Text(
                        text = "${date.dayOfMonth} ${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val topicEnum = Topic.fromId(post.topic)
                    val topicColor = topicEnum.color
                    Text(
                        text = topicEnum.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = topicColor,
                        modifier = Modifier
                            .background(topicColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // İçerik: Markdown Desteği ile
                Markdown(
                    content = post.content,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                // Aksiyon Butonları: Beğen, Yorum, Paylaş
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 32.dp), // Sağdan biraz boşluk bırak
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Yorum
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onClick() }.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = PlanoraIcons.MessageCircle,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        if (post.commentsCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = post.commentsCount.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Beğeni
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Like action */ }.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLikedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        if (post.likesCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = post.likesCount.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (post.isLikedByMe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Paylaş
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Share action */ }.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Bookmark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onBookmarkClick() }.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isBookmarkedByMe) PlanoraIcons.BookmarkFilled else PlanoraIcons.Bookmark,
                            contentDescription = "Bookmark",
                            tint = if (post.isBookmarkedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

fun generateAvatarColor(username: String): Color {
    val hash = username.hashCode().absoluteValue
    val h = (hash % 360).toFloat()
    return Color.hsl(h, 0.65f, 0.40f)
}
