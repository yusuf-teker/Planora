package com.yusufteker.planora.feature.home.presentation.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.LoadStateLoading
import app.cash.paging.compose.collectAsLazyPagingItems
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.yusufteker.planora.feature.home.presentation.util.color
import com.yusufteker.planora.feature.home.presentation.home.components.PostCard
import com.yusufteker.planora.feature.home.presentation.social.components.CommentsBottomSheet
import app.cash.paging.compose.itemContentType
import app.cash.paging.compose.itemKey
import com.yusufteker.planora.core.base.CollectEffect
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import com.yusufteker.planora.core.navigation.Screen
import org.jetbrains.compose.resources.stringResource
import planora.core.generated.resources.Res
import planora.core.generated.resources.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    viewModel: SocialViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    val feedItems = viewModel.feedPagingData.collectAsLazyPagingItems()

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Sadece manuel refresh bittiğinde spinner'ı kaldırmak için dinliyoruz
    LaunchedEffect(feedItems.loadState.refresh) {
        val isLoading = feedItems.loadState.refresh is LoadStateLoading //MediaStore'dan veri geliyorsa true
        if (!isLoading && isRefreshing) {
            // Yenileme işlemi bittiğinde (çok hızlı bitse bile) yarım saniye animasyon devam etsin
            delay(1.seconds)
            isRefreshing = false
        }
    }

    viewModel.effect.CollectEffect { effect ->
        // Handle effects
    }

    // Instagram tarzı pürüzsüz animasyon offset'i
    val animatedOffset by animateFloatAsState(
        targetValue = when {
            isRefreshing -> 140f
            pullToRefreshState.distanceFraction > 0f -> pullToRefreshState.distanceFraction * 140f
            else -> 0f
        },
        label = "refresh_offset"
    )

    val rootNavigator = com.yusufteker.planora.core.navigation.LocalNavigator.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (state.isLoggedIn) {
                FloatingActionButton(
                    onClick = { rootNavigator.navigate(Screen.CreatePost()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Yeni Post"
                    )
                }
            }
        }
    ) { paddingValues ->
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.background)
        )
        
        if (!state.isLoggedIn) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(Res.string.info_login_required_social),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { rootNavigator.navigate(Screen.Login) }) {
                        Text(stringResource(Res.string.action_login))
                    }
                }
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { 
                    isRefreshing = true // Kullanıcı manuel olarak yenileme yaptığında spinner'ı göster
                    feedItems.refresh() 
                },
                state = pullToRefreshState,
                indicator = {
                    // Varsayılan oklu indicator'ı tamamen gizliyoruz
                },
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = animatedOffset
                        },
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        val allTopics = listOf(null) + com.yusufteker.planora.feature.home.domain.model.Topic.entries
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(allTopics, key = { it?.id ?: "ALL" }) { topic ->
                                val chipColor = topic?.color ?: MaterialTheme.colorScheme.primary
                                FilterChip(
                                    selected = state.selectedTopic == topic?.id,
                                    onClick = { viewModel.onEvent(SocialEvent.OnTopicSelected(topic?.id)) },
                                    label = { Text(topic?.displayName ?: stringResource(Res.string.all_topics)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                        selectedLabelColor = chipColor,
                                        selectedLeadingIconColor = chipColor
                                    )
                                )
                            }
                        }
                    }

                    items(
                        count = feedItems.itemCount,
                        key = feedItems.itemKey { it.id },
                        contentType = feedItems.itemContentType { "Post" }
                    ) { index ->
                        val post = feedItems[index]
                        if (post != null) {
                            PostCard(
                                post = post,
                                onClick = { viewModel.onEvent(SocialEvent.OnPostClicked(post)) },
                                onBookmarkClick = { viewModel.onEvent(SocialEvent.OnBookmarkClicked(post.id)) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // 3. YÜKLEME DURUMLARININ KONTROLÜ (Loading States):
                    feedItems.loadState.apply {
                        when {
                            refresh is LoadStateLoading && feedItems.itemCount == 0 -> {
                                // İlk açılışta veritabanı tamamen boşsa ekranın ortasında standart loading çıkar
                                item {
                                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            refresh is app.cash.paging.LoadStateError -> {
                                item {
                                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(stringResource(Res.string.error_occurred, (refresh as app.cash.paging.LoadStateError).error.message ?: ""))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(onClick = { feedItems.retry() }) {
                                                Text(stringResource(Res.string.action_retry))
                                            }
                                        }
                                    }
                                }
                            }
                            append is LoadStateLoading -> {
                                // Sayfanın en altına inildiğinde yeni veriler gelirken altta çıkan loading
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            append is app.cash.paging.LoadStateError -> {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(stringResource(Res.string.error_failed_to_load_more), color = MaterialTheme.colorScheme.error)
                                            TextButton(onClick = { feedItems.retry() }) {
                                                Text(stringResource(Res.string.action_retry))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Bizim ozel dumduz, oklu olmayan Instagram tarzi yukleme ikonumuz
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = animatedOffset - 90f // Listenin 90f ustunde, asagi dogru iner
                            alpha = (animatedOffset / 140f).coerceIn(0f, 1f)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { (pullToRefreshState.distanceFraction).coerceIn(0f, 1f) },
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    }

    if (state.selectedPostForComments != null) {
        CommentsBottomSheet(
            post = state.selectedPostForComments!!,
            comments = state.comments,
            isLoading = state.isCommentsLoading,
            replyToComment = state.replyToComment,
            onDismissRequest = { viewModel.onEvent(SocialEvent.OnCloseComments) },
            onReplyClicked = { viewModel.onEvent(SocialEvent.OnReplyClicked(it)) },
            onSubmitComment = { viewModel.onEvent(SocialEvent.OnSubmitComment(it)) }
        )
    }
}
