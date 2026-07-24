package com.yusufteker.planora.feature.home.presentation.social.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.planora.feature.home.domain.model.Comment
import com.yusufteker.planora.feature.home.domain.model.Post
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.CircularProgressIndicator
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.*

/**
 * Bu bileşen Jetpack Compose kullanarak gönderiye ait yorumları ekranın altından 
 * kayarak çıkan bir panelde (Bottom Sheet) gösterir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    comments: List<Comment>,
    isLoading: Boolean,
    replyToComment: Comment?,
    onDismissRequest: () -> Unit,
    onReplyClicked: (Comment) -> Unit,
    onSubmitComment: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var textValue by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(0.85f) // Yüksekliği içeriğe veriyoruz, bottom sheet'in kendisine değil
        ) {
            Text(
                text = stringResource(Res.string.title_comments),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            HorizontalDivider()

            // Ekranın orta bölümü: Yorum Listesi
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (comments.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.empty_comments_prompt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    // LazyColumn, Android'deki RecyclerView'in karşılığıdır.
                    // Yalnızca ekranda görünen öğeleri çizerek performansı artırır.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentItem(
                                comment = comment,
                                onReplyClicked = { onReplyClicked(it) }
                            )
                        }
                    }
                }
            }

            // Ekranın Alt Kısmı: Yorum Yazma Alanı (Input Area)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (replyToComment != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.replying_to, replyToComment.authorName),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { /* ViewModel'a iptal eventi gönderilecek, şimdilik sadece UI */ }) {
                                Text(stringResource(Res.string.cancel))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(Res.string.add_comment_placeholder)) },
                            shape = CircleShape,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (textValue.isNotBlank()) {
                                    onSubmitComment(textValue)
                                    textValue = ""
                                }
                            },
                            enabled = textValue.isNotBlank(),
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(Res.string.action_send),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onReplyClicked: (Comment) -> Unit,
    isReply: Boolean = false // Eğer bu bir yanıtsa (alt yorumsa) sol taraftan daha fazla boşluk (padding) bırakacağız.
) {
    // İç içe yanıtların (replies) gösterilip gösterilmeyeceğini tutan Local State (Yerel Durum)
    var showReplies by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(start = if (isReply) 32.dp else 0.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(if (isReply) 24.dp else 32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.authorName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatTimeAgo(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (comment.isSending) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
                
                if (comment.error != null) {
                    Text(
                        text = stringResource(Res.string.error_failed_to_send),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (!comment.isSending && !isReply) {
                    TextButton(
                        onClick = { onReplyClicked(comment) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.action_reply),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Yanıtlar Kısmı (Nested Comments)
        if (comment.replies.isNotEmpty()) {
            if (showReplies) {
                Spacer(modifier = Modifier.height(8.dp))
                comment.replies.forEach { reply ->
                    // Aynı bileşeni (CommentItem) yanıtlar için de çağırıyoruz (Recursive mantık).
                    CommentItem(
                        comment = reply,
                        onReplyClicked = onReplyClicked,
                        isReply = true // Yanıt olduğunu belirtiyoruz
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                // Yanıtlar kapalıysa sadece "X yanıtı görüntüle" butonu gösterilir.
                TextButton(
                    onClick = { showReplies = true },
                    contentPadding = PaddingValues(start = 44.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.view_replies, comment.replies.size.toString()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun formatTimeAgo(timestampMillis: Long): String {
    // Simple placeholder formatting
    return stringResource(Res.string.time_just_now)
}
