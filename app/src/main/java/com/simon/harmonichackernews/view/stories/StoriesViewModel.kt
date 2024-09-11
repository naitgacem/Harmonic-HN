package com.simon.harmonichackernews.view.stories

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simon.harmonichackernews.data.FirebaseRepository
import com.simon.harmonichackernews.data.NetworkResult
import com.simon.harmonichackernews.data.Story
import com.simon.harmonichackernews.data.StoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {
    private var idList: List<Int>? = null
    private var storyType = StoryType.TOP
    val stories = MutableLiveData<List<Story>>(mutableListOf())
    private var page = 0
    private val pageSize = 30
    private var loadingStoriesJob: Job? = null
    private val fakeData = List(5) { _ ->
        Story("Loading", -1, false, false)
    }

    val isRefreshing = MutableLiveData(false)
    val loadingFailed = MutableLiveData(false)


    private var attempts = 3

    init {
        loadMore()
    }

    private suspend fun loadListId() {
        val result = firebaseRepository.getTopStories(storyType)
        idList = if (result is NetworkResult.Success) {
            result.data
        } else {
            null
        }
    }

    private fun loadStories() {
        isRefreshing.value = true
        loadingStoriesJob = viewModelScope.launch {
            if (idList == null) {
                loadListId()
            }
            if (idList == null) {
                loadingFailed.value = true
                isRefreshing.value = false
                return@launch
            }
            idList!!.let {
                val oldList = if (loadingFailed.value == true) {
                    synchronized(this) {
                        if (attempts <= 0) {
                            isRefreshing.value = false
                            return@launch
                        }
                        attempts -= 1
                        loadingFailed.value = false
                        stories.value?.dropLast(fakeData.size) ?: emptyList()
                    }
                } else {
                    stories.value ?: emptyList()
                }

                val startIndex = page * pageSize
                fakeData.forEachIndexed { index, story ->
                    story.id = it[minOf(startIndex + index, it.size - 1)]
                }
                stories.value = oldList + fakeData

                val result = firebaseRepository.getStories(it, page, pageSize)
                isRefreshing.value = false
                if (result.any { !it.loaded }) {
                    loadingFailed.value = true
                    return@launch
                }
                stories.value = oldList + result
                page += 1
            }
        }
    }

    fun loadMore() {
        if (isRefreshing.value != true) {
            isRefreshing.value = true
            loadStories()
        }
    }

    fun refreshError() {
        synchronized(this) {
            attempts = 3
        }
        loadMore()
    }

    fun updateStoryType(storyType: StoryType) {
        this.storyType = storyType
        refresh()
    }

    private fun refresh() {
        loadingStoriesJob?.cancel()
        idList = null
        isRefreshing.value = false
        loadingFailed.value = false
        page = 0
        attempts = 3
        stories.value = emptyList()
        loadMore()
    }
}