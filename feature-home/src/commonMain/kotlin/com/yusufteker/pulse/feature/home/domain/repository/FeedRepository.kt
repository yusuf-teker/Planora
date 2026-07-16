package com.yusufteker.pulse.feature.home.domain.repository

import app.cash.paging.ExperimentalPagingApi
import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.PagingSource
import app.cash.paging.PagingState
import app.cash.paging.map
import app.cash.sqldelight.Query
import com.yusufteker.pulse.core.database.PostEntity
import com.yusufteker.pulse.core.database.PulsyDatabase
import com.yusufteker.pulse.core.preferences.SessionPreferences
import com.yusufteker.pulse.feature.home.data.api.FeedApi
import com.yusufteker.pulse.feature.home.data.paging.FeedRemoteMediator
import com.yusufteker.pulse.feature.home.domain.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.max

// 1. YEREL VERİTABANI SAYFALAMA KAYNAĞI (PagingSource):
// Paging 3 kütüphanesi veritabanından veri okumak için bu sınıfı kullanır.
class FeedPagingSource(
    private val localDatabase: PulsyDatabase,
    private val topic: String?,
    private val ownerId: String
) : PagingSource<Int, PostEntity>(), Query.Listener {

    private val query = localDatabase.pulsyDatabaseQueries.getAllPosts(ownerId = ownerId, topic = topic, limit = 0, offset = 0)

    init {
        query.addListener(this)
        registerInvalidatedCallback {
            query.removeListener(this)
        }
    }

    override fun queryResultsChanged() {
        invalidate()
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PostEntity> {
        return withContext(Dispatchers.IO) {
            try {
                val key = params.key ?: 0
                val limit = params.loadSize.toLong()
                val offset = key.toLong()

                val count = localDatabase.pulsyDatabaseQueries.countAllPosts(ownerId = ownerId, topic = topic).executeAsOne()
                val data = localDatabase.pulsyDatabaseQueries.getAllPosts(ownerId = ownerId, topic = topic, limit = limit, offset = offset).executeAsList()

                val nextKey = if (offset + data.size >= count) null else key + data.size
                val prevKey = if (key <= 0) null else max(0, key - params.loadSize)

                LoadResult.Page(
                    data = data,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, PostEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(state.config.pageSize) ?: anchorPage?.nextKey?.minus(state.config.pageSize)
        }
    }
}

interface FeedPagingSourceFactory {
    fun create(topic: String?, ownerId: String): PagingSource<Int, PostEntity>
}

class DefaultFeedPagingSourceFactory(
    private val localDatabase: PulsyDatabase
) : FeedPagingSourceFactory {
    override fun create(topic: String?, ownerId: String): PagingSource<Int, PostEntity> {
        return FeedPagingSource(localDatabase, topic, ownerId)
    }
}

// 2. REPOSITORY PATTERN
// Temiz Mimari'nin kalbidir. SocialViewModel veriyi doğrudan API veya Veritabanından istemez,
// Gelip bu sınıftan (Repository) ister. Repository, verinin nereden alınacağını koordine eder.
class FeedRepository(
    private val localDatabase: PulsyDatabase,
    private val feedApi: FeedApi,
    private val sessionPreferences: SessionPreferences,
    private val pagingSourceFactory: FeedPagingSourceFactory
) {

    @OptIn(ExperimentalPagingApi::class, ExperimentalCoroutinesApi::class)
    fun getFeed(topic: String?): Flow<PagingData<Post>> {
        return sessionPreferences.userIdFlow.flatMapLatest { userId ->
            val ownerId = userId ?: "guest"
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    initialLoadSize = 20
                ),
                remoteMediator = FeedRemoteMediator(
                    localDatabase,
                    feedApi,
                    topic,
                    ownerId
                ),
                pagingSourceFactory = {
                    pagingSourceFactory.create(topic, ownerId)
                }
            )
            .flow
            .map { pagingData ->
                pagingData.map { entity ->
                    Post(
                        id = entity.id,
                        authorId = entity.authorId,
                        authorName = entity.authorName,
                        authorUsername = entity.authorUsername,
                        content = entity.content,
                        createdAt = entity.createdAt,
                        likesCount = entity.likesCount.toInt(),
                        commentsCount = entity.commentsCount.toInt(),
                        isLikedByMe = entity.isLikedByMe == 1L,
                        isBookmarkedByMe = entity.isBookmarkedByMe == 1L,
                        topic = entity.topic
                    )
                }
            }
        }
    }

    suspend fun toggleBookmark(postId: String): Result<Unit> {
        val ownerId = sessionPreferences.getOwnerId()
        val currentPost = localDatabase.pulsyDatabaseQueries.getAllPosts(ownerId = ownerId, topic = null, limit = 1, offset = 0)
            .executeAsList().find { it.id == postId }
            
        if (currentPost != null) {
            val newStatus = if (currentPost.isBookmarkedByMe == 1L) 0L else 1L
            // Optimistically update DB
            localDatabase.pulsyDatabaseQueries.insertPost(
                id = currentPost.id,
                ownerId = currentPost.ownerId,
                authorId = currentPost.authorId,
                authorName = currentPost.authorName,
                authorUsername = currentPost.authorUsername,
                content = currentPost.content,
                createdAt = currentPost.createdAt,
                likesCount = currentPost.likesCount,
                commentsCount = currentPost.commentsCount,
                isLikedByMe = currentPost.isLikedByMe,
                isBookmarkedByMe = newStatus,
                topic = currentPost.topic
            )
        }
        
        return feedApi.toggleBookmark(postId).onFailure {
            // Revert DB on failure
            if (currentPost != null) {
                localDatabase.pulsyDatabaseQueries.insertPost(
                    id = currentPost.id,
                    ownerId = currentPost.ownerId,
                    authorId = currentPost.authorId,
                    authorName = currentPost.authorName,
                    authorUsername = currentPost.authorUsername,
                    content = currentPost.content,
                    createdAt = currentPost.createdAt,
                    likesCount = currentPost.likesCount,
                    commentsCount = currentPost.commentsCount,
                    isLikedByMe = currentPost.isLikedByMe,
                    isBookmarkedByMe = currentPost.isBookmarkedByMe,
                    topic = currentPost.topic
                )
            }
        }
    }
}