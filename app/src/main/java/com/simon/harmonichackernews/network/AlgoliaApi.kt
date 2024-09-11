package com.simon.harmonichackernews.network

import com.simon.harmonichackernews.data.PostType
import com.simon.harmonichackernews.data.SortType
import com.simon.harmonichackernews.data.Story
import javax.inject.Inject


class AlgoliaApi @Inject constructor(
    private val algoliaService: AlgoliaService
) {
    suspend fun search(
        query: String,
        sortType: SortType = SortType.BY_RELEVANCE,
        postTypes: List<PostType> = listOf(), // TODO Currently ORing them
        frontPage: Boolean = false,
        author: String? = null,
    ): List<Story> {
        val tags = generateTags(postTypes, frontPage, author)
        val response = when (sortType) {
            SortType.BY_RELEVANCE -> algoliaService.searchByRelevance(query, tags)
            SortType.BY_DATE -> algoliaService.searchByDate(query, tags)
        }
        return JSONParser.algoliaJsonToStories(response.string())
    }

    private fun generateTags(
        postTypes: List<PostType>, frontPage: Boolean, author: String?
    ): String {
        val tagsStringBuilder = StringBuilder()
        if (postTypes.isNotEmpty()) {
            tagsStringBuilder.append("(")
            for (type in postTypes) {
                tagsStringBuilder.append(
                    when (type) {
                        PostType.STORY -> "story"
                        PostType.COMMENT -> "comment"
                        PostType.POLL -> "poll"
                        PostType.SHOW_HN -> "show_hn"
                        PostType.ASK_HN -> "ask_hn"
                    }
                )
                tagsStringBuilder.append(",")
            }
            tagsStringBuilder.append("),")
        }
        if (frontPage) {
            tagsStringBuilder.append("front_page")
        }
        if (!author.isNullOrBlank()) {
            tagsStringBuilder.append("author_$author")
        }
        return tagsStringBuilder.toString()
    }
}