package com.yusufteker.pulse.feature.home.presentation.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.yusufteker.pulse.feature.home.presentation.home.components.PostCard
import app.cash.paging.compose.itemContentType
import app.cash.paging.compose.itemKey
import com.yusufteker.pulse.core.base.CollectEffect
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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

    val rootNavigator = com.yusufteker.pulse.core.navigation.LocalNavigator.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Sosyal", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.PendingPosts) }) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.Drafts, contentDescription = "Bekleyenler")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = { rootNavigator.navigate(com.yusufteker.pulse.core.navigation.Screen.CreatePost()) }
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                    contentDescription = "Yeni Post"
                )
            }
        }
    ) { paddingValues ->
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.background)
        )
        
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
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(
                    count = feedItems.itemCount,
                    key = feedItems.itemKey { it.id },
                    contentType = feedItems.itemContentType { "Post" }
                ) { index ->
                    val post = feedItems[index]
                    if (post != null) {
                        Column {
                            PostCard(
                                post = post,
                                onClick = { /* Detail navigation to be added later */ }
                            )
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
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
                        append is LoadStateLoading -> {
                            // Sayfanın en altına inildiğinde yeni veriler gelirken altta çıkan loading
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
            
            // Bizim ozel dumduz, oklu olmayan Instagram tarzi yukleme ikonumuz
            if (animatedOffset > 0f) {
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
}
