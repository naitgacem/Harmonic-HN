package com.simon.harmonichackernews.data

import com.simon.harmonichackernews.network.FirebaseService
import com.simon.harmonichackernews.network.JSONParser
import com.simon.harmonichackernews.network.handleApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class FirebaseRepository @Inject constructor(
    private val firebaseService: FirebaseService
) {

    suspend fun getTopStories(
        storyType: StoryType
    ): NetworkResult<List<Int>> {
        return handleApi {
            when (storyType) {
                StoryType.TOP -> firebaseService.getTopStories()
                StoryType.NEW -> firebaseService.getTopStories()
                StoryType.BEST -> firebaseService.getBestStories()
                StoryType.ASK -> firebaseService.getAskStories()
                StoryType.SHOW -> firebaseService.getShowStories()
                StoryType.JOB -> firebaseService.getJobStories()
            }
        }
    }

    suspend fun getStories(idsList: List<Int>, page: Int, pageSize: Int): List<Story> {
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, idsList.size)
        val deferrables = mutableListOf<Deferred<Story>>()
        coroutineScope {
            for (i in startIndex until endIndex) {
                async {
                    getStory(idsList[i])
                }.let { deferrables.add(it) }
            }
        }
        val storyList = deferrables.awaitAll()
        return storyList
    }

    private suspend fun getStory(
        id: Int
    ): Story {
        val story = Story()
        try {
            val result = firebaseService.getStory(id)
            val responseBodyJson = result.body()?.string()
            JSONParser.updateStoryWithHNJson(responseBodyJson, story)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return story
    }
}