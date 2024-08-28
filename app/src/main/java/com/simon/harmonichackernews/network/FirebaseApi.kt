package com.simon.harmonichackernews.network

import com.simon.harmonichackernews.data.NetworkResult
import com.simon.harmonichackernews.data.Story
import com.simon.harmonichackernews.data.StoryType
import java.io.IOException
import javax.inject.Inject

class FirebaseApi @Inject constructor(
    private val firebaseService: FirebaseService
) {


    suspend fun getStory(
        id: Int
    ): NetworkResult<Story> {
        val result = handleApi {
            firebaseService.getStory(id)
        }
        return when (result) {
            is NetworkResult.Error -> NetworkResult.Error(result.code)
            is NetworkResult.Exception -> NetworkResult.Exception(result.e)
            is NetworkResult.Success -> {
                val responseBodyJson = result.data.string()
                val story = Story()
                val success = JSONParser.updateStoryWithHNJson(responseBodyJson, story)
                if (success) {
                    return NetworkResult.Success(story)
                } else {
                    return NetworkResult.Exception(IOException())
                }
            }
        }
    }

    suspend fun getStory2(
        id: Int
    ): Story {
        val result = firebaseService.getStory(id)
        val responseBodyJson = result.body()?.string()
        val story = Story()
        val success = JSONParser.updateStoryWithHNJson(responseBodyJson, story)
        if (success) {
            return story
        } else {
            throw IOException()
        }
    }
}