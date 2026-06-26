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
import com.yusufteker.pulse.core.database.PulseDatabase
import com.yusufteker.pulse.feature.home.data.api.FeedApi
import com.yusufteker.pulse.feature.home.data.paging.FeedRemoteMediator
import com.yusufteker.pulse.feature.home.domain.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.max

class FeedPagingSource(
    private val database: PulseDatabase
) : PagingSource<Int, PostEntity>(), Query.Listener {

    private val query = database.pulseDatabaseQueries.getAllPosts(0, 0)

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

                val count = database.pulseDatabaseQueries.countAllPosts().executeAsOne()
                val data = database.pulseDatabaseQueries.getAllPosts(limit, offset).executeAsList()

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

class FeedRepository(
    private val database: PulseDatabase,
    private val feedApi: FeedApi
) {

    @OptIn(ExperimentalPagingApi::class)
    fun getFeed(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20
            ),
            remoteMediator = FeedRemoteMediator(
                database,
                feedApi
            ),
            pagingSourceFactory = {
                FeedPagingSource(database)
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
                        isLikedByMe = entity.isLikedByMe == 1L
                    )
                }
            }
    }
}