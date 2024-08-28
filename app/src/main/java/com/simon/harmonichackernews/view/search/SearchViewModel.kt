package com.simon.harmonichackernews.view.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simon.harmonichackernews.data.PostType
import com.simon.harmonichackernews.data.SortType
import com.simon.harmonichackernews.data.Story
import com.simon.harmonichackernews.network.AlgoliaApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val algoliaApi: AlgoliaApi
) : ViewModel() {
    private var _searchResults = MutableLiveData<List<Story>>()
    val searchResults: LiveData<List<Story>> = _searchResults
    private var _loading = MutableLiveData(false);
    val loading: LiveData<Boolean> = _loading
    private var postTypes: List<PostType> = emptyList()
    private var sortType = SortType.BY_RELEVANCE
    private var isFrontPage = false
    private var author: String? = null
    fun search(query: String){
        _loading.postValue(true)
        viewModelScope.launch {
            val result = algoliaApi.search(query, postTypes = postTypes, frontPage = isFrontPage, author = author)
            _loading.postValue(false)
            _searchResults.postValue(result)
        }
    }

    fun setPostTypes(postTypes: List<PostType>){
        this.postTypes = postTypes
    }
    fun setSortType(sortType: SortType){
        this.sortType = sortType
    }
    fun setFrontPage(isFrontPage: Boolean){
        this.isFrontPage = isFrontPage
    }
    fun setAuthor(author: String?){
        this.author = author
    }
}