package com.simon.harmonichackernews.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.simon.harmonichackernews.data.Story
import com.simon.harmonichackernews.network.FirebaseApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.IOException

class FirebasePagingSource(
    private val firebaseApi: FirebaseApi,
    private val getList: () -> List<Int>?,
) : PagingSource<Int, Story>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Story> {
        val list = getList() ?: return LoadResult.Error(IOException())
        val page = (params.key) ?: 0
        val startIndex = page * PAGE_SIZE
        val endIndex = ((page + 1) * PAGE_SIZE).coerceAtMost(list.size)
        val deferrables = mutableListOf<Deferred<Story?>>()
        coroutineScope {
            for (i in startIndex until endIndex) {
               async {
                   try {
                       firebaseApi.getStory2(list[i])
                   } catch (e: Exception) {
                       null
                   }
               }.let { deferrables.add(it) }
            }
        }
        val storyList = deferrables.awaitAll()
        if(storyList.contains(null)){
            return LoadResult.Error(IOException())
        }

        val nextKey = if (endIndex - startIndex < PAGE_SIZE) null else page + 1
        val prevKey = if (page == 0) null else page - 1

        return LoadResult.Page(
            data = storyList.filterNotNull(),
            prevKey = prevKey,
            nextKey = nextKey,
            itemsBefore = startIndex,
            itemsAfter = (list.size - endIndex).coerceIn(0, 5)
        )
    }

    override fun getRefreshKey(state: PagingState<Int, Story>): Int? {
        return state.anchorPosition?.let {
            it / PAGE_SIZE
        }
    }

    companion object {
        const val PAGE_SIZE = 10
    }
}