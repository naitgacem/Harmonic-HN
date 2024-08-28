package com.simon.harmonichackernews.data

enum class SortType {
    /**
     * Sorted by relevance, then points, then number of comments
     */
    BY_RELEVANCE,

    /**
     * Sorted by date, more recent first
     *
     */
    BY_DATE,
}

enum class PostType {
    STORY,
    COMMENT,
    POLL,
    POLL_OPTION,
    SHOW_HN,
    ASK_HN,
}

enum class StoryType {
    TOP,
    NEW,
    BEST,
    ASK,
    SHOW,
    JOB
}