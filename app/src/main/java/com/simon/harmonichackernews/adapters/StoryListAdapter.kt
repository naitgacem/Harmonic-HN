package com.simon.harmonichackernews.adapters

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.simon.harmonichackernews.R
import com.simon.harmonichackernews.data.Story
import com.simon.harmonichackernews.databinding.StoryListItemBinding
import com.simon.harmonichackernews.network.FaviconLoader
import com.simon.harmonichackernews.utils.FontUtils
import com.simon.harmonichackernews.utils.SettingsUtils
import com.simon.harmonichackernews.utils.Utils
import java.net.URI
import java.text.MessageFormat

class StoryListAdapter(
    context: Context,
    private val onLinkClick: (Story) -> Unit = {},
    private val onCommentsCLick: (Story) -> Unit = {},
) : ListAdapter<Story, StoriesViewHolder>(StoryDiff) {
    private val leftAlign = SettingsUtils.shouldUseLeftAlign(context)
    private val showIndex = SettingsUtils.shouldShowIndex(context)
    private val showCommentsCount = SettingsUtils.shouldShowCommentsCount(context)
    private val showPoints = SettingsUtils.shouldShowPoints(context)
    private val thumbnails = SettingsUtils.shouldShowThumbnails(context)
    private val faviconProvider = SettingsUtils.getPreferredFaviconProvider(context)
    private val hotness = SettingsUtils.getPreferredHotness(context)
    private val compactView = SettingsUtils.shouldUseCompactView(context)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoriesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            if (leftAlign) R.layout.story_list_item_left else R.layout.story_list_item,
            parent,
            false
        )
        return StoriesViewHolder(view, onLinkClick)

    }

    override fun onBindViewHolder(holder: StoriesViewHolder, position: Int) {
        val story = getItem(position)
        holder.currentItem = story
        val binding = StoryListItemBinding.bind(holder.itemView)
        val ctx = holder.itemView.context
        if (story.loaded) {
            // actual item
            if (showIndex) {
                binding.storyIndex.text = ctx.getString(R.string.story_index, position + 1)
                binding.storyIndex.setTextColor(
                    Utils.getColorViaAttr(
                        ctx,
                        if (story.clicked) R.attr.storyColorDisabled else R.attr.storyColorNormal
                    )
                )
                binding.storyIndex.setTextSize(
                    TypedValue.COMPLEX_UNIT_DIP, if (position < 100) 16f else 13f
                )
                binding.storyIndex.setPadding(
                    0, Utils.pxFromDpInt(
                        ctx.resources, if (position < 100) 2.2f else 5.3f
                    ), 0, 0
                )
            }
            binding.storyIndex.isVisible = showIndex

            val title = if (story.pdfTitle.isNullOrBlank()) {
                story.title
            } else {
                val spannable = SpannableStringBuilder(story.pdfTitle).append(" ")
                val drawableRes = if (story.clicked) {
                    R.drawable.ic_action_pdf_clicked
                } else {
                    R.drawable.ic_action_pdf
                }
                val imageSpan = ImageSpan(ctx, drawableRes)
                spannable.setSpan(
                    imageSpan,
                    spannable.length - 1,
                    spannable.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable
            }
            binding.storyTitle.text = title
            val commentCountText =
                if (showCommentsCount) story.descendants.toString() else if (story.descendants > 0) ctx.getString(
                    R.string.dot
                ) else ""
            binding.storyComments.text = commentCountText

            val host = try {
                URI.create(story.url).host
            } catch (e: Exception) {
                ctx.getString(R.string.unknown_host)
            }

            if (showPoints) {
                val metaStringTemplate = ctx.getString(R.string.story_meta)
                binding.storyMeta.text =
                    MessageFormat.format(metaStringTemplate, story.score, host, story.timeFormatted)
            }
            if (thumbnails) {
                FaviconLoader.loadFavicon(story.url, binding.storyMetaFavicon, ctx, faviconProvider)
            }
            val isHot = hotness > 0 && (story.score + story.descendants) > hotness
            binding.storyCommentsIcon.setImageResource(if (isHot) R.drawable.ic_action_whatshot else R.drawable.ic_action_comment)

            FontUtils.setTypeface(binding.storyTitle, true, 17.5f, 18F, 16F, 17F, 17F, 18F);
            FontUtils.setTypeface(binding.storyMeta, false, 13F, 13F, 12F, 12F, 13F, 13F);
            FontUtils.setTypeface(binding.storyComments, true, 14F, 13F, 13F, 14F, 14F, 14F);

            val alpha = if (story.clicked) 0.6f else 1.0f
            val textColor = if (story.clicked) R.attr.textColorDisabled else R.attr.textColorDefault
            val storyColor =
                if (story.clicked) R.attr.storyColorDisabled else R.attr.storyColorNormal
            with(binding) {
                storyTitle.setTextColor(Utils.getColorViaAttr(ctx, storyColor))
                storyCommentsIcon.alpha = alpha
                storyMetaFavicon.alpha = alpha
                storyComments.setTextColor(Utils.getColorViaAttr(ctx, textColor))
                storyMeta.setTextColor(Utils.getColorViaAttr(ctx, textColor))
            }

            with(binding) {
                storyTitleShimmer.isVisible = false
                storyTitleShimmerMeta.isVisible = false
                storyTitle.isVisible = true
                storyMetaContainer.isInvisible = compactView
                storyComments.isInvisible = compactView
                storyMetaFavicon.isVisible = thumbnails
            }

            binding.storyLinkLayout.setOnClickListener {
                onLinkClick(story)
                notifyItemChanged(position)
            }
            binding.storyCommentLayout.setOnClickListener {
                onCommentsCLick(story)
                notifyItemChanged(position)
            }

        } else {
            // placeholder
            with(binding) {
                storyCommentsIcon.setImageResource(R.drawable.ic_action_comment)
                storyTitleShimmer.isVisible = true
                storyTitleShimmerMeta.isInvisible = compactView
                storyTitle.isVisible = false
                storyMetaContainer.isVisible = false
                storyComments.text = null
                storyLinkLayout.isClickable = false
                storyCommentLayout.isClickable = false
                storyCommentsIcon.alpha = if (story.clicked) 0.6f else 1.0f
            }
        }
    }

}

class StoriesViewHolder(mView: View, onLinkClick: (Story) -> Unit) :
    RecyclerView.ViewHolder(mView) {
    val linkLayoutView: LinearLayout = mView.findViewById(R.id.story_link_layout)
    val commentLayoutView: LinearLayout = mView.findViewById(R.id.story_comment_layout)
    var currentItem: Story? = null


    private var touchX = 0
    private var touchY = 0

    init {
        commentLayoutView.setOnClickListener { v: View? ->
//            commentClickListener.onItemClick(
//                absoluteAdapterPosition
//            )
        }

//        if (longClickListener != null) {
//            linkLayoutView.setOnTouchListener { v, event ->
//                touchX = event.x.toInt()
//                touchY = event.y.toInt()
//                false
//            }
//        }

//        linkLayoutView.setOnLongClickListener { v: View? ->
//            longClickListener.onLongClick(
//                v,
//                absoluteAdapterPosition,
//                touchX,
//                touchY
//            )
//        }
    }
}


object StoryDiff : DiffUtil.ItemCallback<Story>() {
    override fun areContentsTheSame(
        oldItem: Story, newItem: Story
    ): Boolean {
        return oldItem == newItem
    }

    override fun areItemsTheSame(
        oldItem: Story, newItem: Story
    ): Boolean {
        return oldItem.id == newItem.id
    }
}
