package com.simon.harmonichackernews.utils;

import android.content.Context;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.simon.harmonichackernews.CommentsFragment;
import com.simon.harmonichackernews.R;
import com.simon.harmonichackernews.adapters.CommentsRecyclerViewAdapter;
import com.simon.harmonichackernews.data.Comment;
import com.simon.harmonichackernews.data.Story;

import java.util.ArrayList;
import java.util.List;

public class CommentsUtils {
    public final static String EXTRA_TITLE = "com.simon.harmonichackernews.EXTRA_TITLE";
    public final static String EXTRA_PDF_TITLE = "com.simon.harmonichackernews.EXTRA_PDF_TITLE";
    public final static String EXTRA_BY = "com.simon.harmonichackernews.EXTRA_BY";
    public final static String EXTRA_URL = "com.simon.harmonichackernews.EXTRA_URL";
    public final static String EXTRA_TIME = "com.simon.harmonichackernews.EXTRA_TIME";
    public final static String EXTRA_KIDS = "com.simon.harmonichackernews.EXTRA_KIDS";
    public final static String EXTRA_POLL_OPTIONS = "com.simon.harmonichackernews.EXTRA_POLL_OPTIONS";
    public final static String EXTRA_DESCENDANTS = "com.simon.harmonichackernews.EXTRA_DESCENDANTS";
    public final static String EXTRA_ID = "com.simon.harmonichackernews.EXTRA_ID";
    public final static String EXTRA_SCORE = "com.simon.harmonichackernews.EXTRA_SCORE";
    public final static String EXTRA_TEXT = "com.simon.harmonichackernews.EXTRA_TEXT";
    public final static String EXTRA_IS_LINK = "com.simon.harmonichackernews.EXTRA_IS_LINK";
    public final static String EXTRA_IS_COMMENT = "com.simon.harmonichackernews.EXTRA_IS_COMMENT";
    public final static String EXTRA_PARENT_ID = "com.simon.harmonichackernews.EXTRA_PARENT_ID";
    public final static String EXTRA_FORWARD = "com.simon.harmonichackernews.EXTRA_FORWARD";
    public final static String EXTRA_SHOW_WEBSITE = "com.simon.harmonichackernews.EXTRA_SHOW_WEBSITE";

    public static List<Triplet<String, Integer, CommentAction>> getPossibleActions(Comment comment) {
        List<Triplet<String, Integer, CommentAction>> itemsList = new ArrayList<>();

        itemsList.add(new Triplet<>("View user (" + comment.by + ")", R.drawable.ic_action_user, CommentAction.VIEW_USER));
        itemsList.add(new Triplet<>("Share comment link", R.drawable.ic_action_share, CommentAction.SHARE_COMMENT_LINK));
        itemsList.add(new Triplet<>("Copy text", R.drawable.ic_action_copy, CommentAction.COPY_TEXT));
        itemsList.add(new Triplet<>("Select text", R.drawable.ic_action_select, CommentAction.SELECT_TEXT));
        itemsList.add(new Triplet<>("Vote up", R.drawable.ic_action_thumbs_up, CommentAction.VOTE_UP));
        itemsList.add(new Triplet<>("Unvote", R.drawable.ic_action_thumbs, CommentAction.UN_VOTE));
        itemsList.add(new Triplet<>("Vote down", R.drawable.ic_action_thumb_down, CommentAction.VOTE_DOWN));
        itemsList.add(new Triplet<>("Bookmark", R.drawable.ic_action_bookmark_border, CommentAction.BOOKMARK));
        if(comment.parentComment != null){
            itemsList.add(new Triplet<>("Parent", R.drawable.ic_action_arrow_up, CommentAction.PARENT_COMMENT));
        }
        if(comment.rootComment != null){
            itemsList.add(new Triplet<>("Root", R.drawable.ic_action_arrow_up, CommentAction.ROOT_COMMENT));
        }
        if(!Utils.timeInSecondsMoreThanTwoWeeksAgo(comment.time)){
            itemsList.add(new Triplet<>("Reply", R.drawable.ic_action_reply, CommentAction.REPLY));
        }
        return itemsList;
    }


    public static void fillStoryFromBundle(Story story, Bundle bundle) {
        story.title = bundle.getString(EXTRA_TITLE);
        story.pdfTitle = bundle.getString(EXTRA_PDF_TITLE, null);
        story.by = bundle.getString(EXTRA_BY);
        story.url = bundle.getString(EXTRA_URL);
        story.time = bundle.getInt(EXTRA_TIME, 0);
        story.kids = bundle.getIntArray(EXTRA_KIDS);
        story.pollOptions = bundle.getIntArray(EXTRA_POLL_OPTIONS);
        story.descendants = bundle.getInt(EXTRA_DESCENDANTS, 0);
        story.id = bundle.getInt(EXTRA_ID, 0);
        story.parentId = bundle.getInt(EXTRA_PARENT_ID, 0);
        story.score = bundle.getInt(EXTRA_SCORE, 0);
        story.text = bundle.getString(EXTRA_TEXT);
        story.isLink = bundle.getBoolean(EXTRA_IS_LINK, true);
        story.isComment = bundle.getBoolean(EXTRA_IS_COMMENT, false);
        story.loaded = true;
    }

    public static void initAdapter(CommentsFragment commentsFragment, CommentsRecyclerViewAdapter adapter, LinearLayout bottomSheet, List<Comment> comments, FrameLayout webViewContainer) {
        if (adapter != null) {
            Context ctx = commentsFragment.requireContext();
            boolean updateHeader = false;
            boolean updateComments = false;

            if (adapter.collapseParent != SettingsUtils.shouldCollapseParent(ctx)) {
                adapter.collapseParent = !adapter.collapseParent;
                updateComments = true;
            }

            if (adapter.showThumbnail != SettingsUtils.shouldShowThumbnails(ctx)) {
                adapter.showThumbnail = !adapter.showThumbnail;
                updateHeader = true;
            }

            if (adapter.preferredTextSize != SettingsUtils.getPreferredCommentTextSize(ctx)) {
                adapter.preferredTextSize = SettingsUtils.getPreferredCommentTextSize(ctx);
                updateHeader = true;
                updateComments = true;
            }

            if (adapter.monochromeCommentDepthIndicators != SettingsUtils.shouldUseMonochromeCommentDepthIndicators(ctx)) {
                adapter.monochromeCommentDepthIndicators = SettingsUtils.shouldUseMonochromeCommentDepthIndicators(ctx);
                updateComments = true;
            }

            if (!adapter.font.equals(SettingsUtils.getPreferredFont(ctx))) {
                adapter.font = SettingsUtils.getPreferredFont(ctx);
                updateHeader = true;
                updateComments = true;
            }

            if (adapter.showTopLevelDepthIndicator != SettingsUtils.shouldShowTopLevelDepthIndicator(ctx)) {
                adapter.showTopLevelDepthIndicator = SettingsUtils.shouldShowTopLevelDepthIndicator(ctx);
                updateComments = true;
            }

            if (adapter.swapLongPressTap != SettingsUtils.shouldSwapCommentLongPressTap(ctx)) {
                adapter.swapLongPressTap = SettingsUtils.shouldSwapCommentLongPressTap(ctx);
            }

            if (!adapter.theme.equals(ThemeUtils.getPreferredTheme(ctx))) {
                adapter.theme = ThemeUtils.getPreferredTheme(ctx);
                updateHeader = true;
                updateComments = true;

                // darkThemeActive might change because the system changed from day to night mode.
                // In that case, we'll need to update the sheet and webview background color since
                // that will have changed too.
                if (bottomSheet != null) {
                    bottomSheet.setBackgroundColor(ContextCompat.getColor(ctx, ThemeUtils.getBackgroundColorResource(ctx)));
                }
                if (webViewContainer != null) {
                    webViewContainer.setBackgroundColor(ContextCompat.getColor(ctx, ThemeUtils.getBackgroundColorResource(ctx)));
                }
            }
            if (updateHeader) {
                adapter.notifyItemChanged(0);
            }
            if (updateComments) {
                adapter.notifyItemRangeChanged(1, comments.size());
            }
        }
    }

    public static void destroyWebView(FrameLayout webViewContainer, WebView webView) {
        //nuclear
        if (webView != null) {
            webViewContainer.removeAllViews();
            webView.clearHistory();
            webView.clearCache(true);
            webView.onPause();
            webView.removeAllViews();
            webView.destroyDrawingCache();
            webView.pauseTimers();
            webView.destroy();
        }
    }
}
