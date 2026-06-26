package com.yusufteker.pulse.feature.home.presentation.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
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
import app.cash.paging.compose.itemContentType
import app.cash.paging.compose.itemKey
import com.yusufteker.pulse.core.base.CollectEffect
import com.yusufteker.pulse.feature.home.presentation.home.components.PostCard

@Composable
fun SocialScreen(
    viewModel: SocialViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val feedItems = viewModel.feedPagingData.collectAsLazyPagingItems()

    viewModel.effect.CollectEffect { effect ->
        // Handle effects
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.background)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize()
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

            feedItems.loadState.apply {
                when {
                    refresh is LoadStateLoading -> {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    append is LoadStateLoading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
