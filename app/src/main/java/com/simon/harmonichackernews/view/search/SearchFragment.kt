package com.simon.harmonichackernews.view.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.simon.harmonichackernews.CommentsActivity
import com.simon.harmonichackernews.R
import com.simon.harmonichackernews.StoriesFragment.StoryClickListener
import com.simon.harmonichackernews.adapters.StoryListAdapter
import com.simon.harmonichackernews.data.PostType
import com.simon.harmonichackernews.data.SortType
import com.simon.harmonichackernews.data.Story
import com.simon.harmonichackernews.databinding.FragmentSearchBinding
import com.simon.harmonichackernews.utils.CommentsUtils
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
        binding.searchView.editText.setOnEditorActionListener { _, _, _ -> search() }

        val clickListener: (Story) -> Unit = { story ->
            (requireActivity() as? StoryClickListener)?.openStory(story, 0, false)
        }
        val storyClickListener: (Story) -> Unit = { story ->
            val intent =
                Intent(requireActivity().applicationContext, CommentsActivity::class.java).apply {
                    putExtra(CommentsUtils.EXTRA_ID, story.commentMasterId)
                    putExtra(CommentsUtils.EXTRA_TITLE, story.commentMasterTitle)
                    putExtra(CommentsUtils.EXTRA_URL, story.commentMasterUrl)
                }
            startActivity(intent)
        }
        val adapter = StoryListAdapter(
            requireContext(),
            onLinkClick = clickListener,
            onCommentsCLick = clickListener,
            onCommentStoryClick = storyClickListener,
            onCommentRepliesClick = clickListener,
        )

        with(binding.recyclerView) {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(DividerItemDecoration(requireContext(), VERTICAL))
            this.adapter = adapter
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.loadingProgressbar.isVisible = loading
        }
        binding.searchBar.setNavigationOnClickListener { v: View -> dismissSearch(v) }
        viewModel.searchResults.observe(viewLifecycleOwner) { stories ->
            binding.noResultsFoundTextview.isVisible = stories.isEmpty()
            adapter.submitList(stories)
        }

        binding.searchSettings.postTypeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val types = checkedIds.mapNotNull { id ->
                when (id) {
                    R.id.story_chip -> PostType.STORY
                    R.id.comment_chip -> PostType.COMMENT
                    R.id.poll_chip -> PostType.POLL
                    R.id.show_hn_chip -> PostType.SHOW_HN
                    R.id.ask_hn_chip -> PostType.ASK_HN
                    else -> null
                }
            }
            viewModel.setPostTypes(types.takeUnless { it.isEmpty() } ?: listOf(PostType.STORY))
        }
        binding.searchSettings.sortTypeToggleGrp.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.by_relevance_btn -> viewModel.setSortType(SortType.BY_RELEVANCE)
                    R.id.by_date_btn -> viewModel.setSortType(SortType.BY_DATE)
                }
            }
        }


        binding.searchSettings.frontPageSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setFrontPage(isChecked)
        }
        binding.searchSettings.usernameEditText.doAfterTextChanged { author ->
            viewModel.setAuthor(author.toString())
        }
        binding.searchSettings.searchBtn.setOnClickListener {
            search()
        }
    }

    private fun search(): Boolean {
        val query = binding.searchView.text.toString()
        binding.searchBar.setText(query)
        viewModel.search(query)
        binding.searchView.hide()
        return false
    }

    private fun dismissSearch(v: View) {
        parentFragmentManager.popBackStack()
    }
}
