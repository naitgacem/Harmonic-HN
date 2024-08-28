package com.simon.harmonichackernews.view.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.simon.harmonichackernews.R
import com.simon.harmonichackernews.StoriesFragment.StoryClickListener
import com.simon.harmonichackernews.adapters.StoryRecyclerViewAdapter
import com.simon.harmonichackernews.adapters.StoryRecyclerViewAdapter.ClickListener
import com.simon.harmonichackernews.data.PostType
import com.simon.harmonichackernews.databinding.FragmentSearchBinding
import com.simon.harmonichackernews.utils.SettingsUtils.getPreferredFaviconProvider
import com.simon.harmonichackernews.utils.SettingsUtils.getPreferredHotness
import com.simon.harmonichackernews.utils.SettingsUtils.shouldShowCommentsCount
import com.simon.harmonichackernews.utils.SettingsUtils.shouldShowIndex
import com.simon.harmonichackernews.utils.SettingsUtils.shouldShowPoints
import com.simon.harmonichackernews.utils.SettingsUtils.shouldShowThumbnails
import com.simon.harmonichackernews.utils.SettingsUtils.shouldUseCompactView
import com.simon.harmonichackernews.utils.SettingsUtils.shouldUseLeftAlign
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private lateinit var binding: FragmentSearchBinding
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).setDuration(600)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).setDuration(600)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.isClickable = true

        val searchBar = binding.searchBar
        val searchView = binding.searchView
        searchView.editText.setOnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
            val query = searchView.text.toString()
            searchBar.setText(query)
            viewModel.search(query)
            searchView.hide()
            false
        }

        val rv = binding.recyclerView
        val adapter = StoryRecyclerViewAdapter(
            listOf(), shouldShowPoints(
                context
            ), shouldShowCommentsCount(context), shouldUseCompactView(
                context
            ), shouldShowThumbnails(context), shouldShowIndex(context), shouldUseLeftAlign(
                context
            ), getPreferredHotness(context), getPreferredFaviconProvider(
                context
            ), null, 0
        )
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(context)
        val clickListener = ClickListener { position ->
            val story = adapter.stories[position]
            (requireActivity() as StoryClickListener).openStory(story, position, false)
        }
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.loadingProgressbar.visibility = if (loading) View.VISIBLE else View.GONE
        }
        binding.searchBar.setNavigationOnClickListener { v: View -> this.dismissSearch(v) }
        viewModel.searchResults.observe(viewLifecycleOwner) { stories ->
            adapter.stories = stories
            adapter.notifyItemRangeChanged(0, adapter.stories.size)
            if (stories.isEmpty()) {
                binding.noResultsFoundTextview.visibility = View.VISIBLE
            } else {
                binding.noResultsFoundTextview.visibility = View.GONE
            }
        }
        adapter.setOnLinkClickListener(clickListener)
        adapter.setOnCommentClickListener(clickListener)

        binding.searchSettings.postTypeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val types = mutableListOf<PostType?>()
            for (id in checkedIds) {
                types.add(
                    when (id) {
                        R.id.story_chip -> PostType.STORY
                        R.id.comment_chip -> PostType.COMMENT
                        R.id.poll_chip -> PostType.POLL
                        R.id.poll_opt_chip -> PostType.POLL_OPTION
                        R.id.show_hn_chip -> PostType.SHOW_HN
                        R.id.ask_hn_chip -> PostType.ASK_HN
                        else -> null
                    }
                )
            }
            viewModel.setPostTypes(types.filterNotNull())
        }

        binding.searchSettings.frontPageSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFrontPage(isChecked)
        }
        binding.searchSettings.usernameEditText.doAfterTextChanged { author ->
            viewModel.setAuthor(author.toString())
        }
    }

    private fun dismissSearch(v: View) {
        parentFragmentManager.popBackStack()
    }
}
