package com.simon.harmonichackernews.view;

import static android.content.Context.DOWNLOAD_SERVICE;
import static androidx.webkit.WebViewFeature.isFeatureSupported;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.simon.harmonichackernews.CommentsFragment;
import com.simon.harmonichackernews.ComposeActivity;
import com.simon.harmonichackernews.R;
import com.simon.harmonichackernews.UserDialogFragment;
import com.simon.harmonichackernews.adapters.CommentsRecyclerViewAdapter;
import com.simon.harmonichackernews.data.Comment;
import com.simon.harmonichackernews.data.CommentsScrollProgress;
import com.simon.harmonichackernews.linkpreview.NitterGetter;
import com.simon.harmonichackernews.network.UserActions;
import com.simon.harmonichackernews.utils.AccountUtils;
import com.simon.harmonichackernews.utils.CommentAction;
import com.simon.harmonichackernews.utils.CommentsUtils;
import com.simon.harmonichackernews.utils.DialogUtils;
import com.simon.harmonichackernews.utils.SettingsUtils;
import com.simon.harmonichackernews.utils.ShareUtils;
import com.simon.harmonichackernews.utils.ThemeUtils;
import com.simon.harmonichackernews.utils.Triplet;
import com.simon.harmonichackernews.utils.Utils;

import java.util.List;

public class ViewUtils {
    public static int findFirstVisiblePosition(LinearLayoutManager layoutManager, int topInset) {
        int firstVisible = layoutManager.findFirstVisibleItemPosition();

        View firstVisibleView = layoutManager.findViewByPosition(firstVisible);
        if (firstVisibleView != null) {
            int top = firstVisibleView.getTop();
            int height = firstVisibleView.getHeight();
            int scrolled = height - Math.abs(top);

            //there is a topInset-sized padding at the top of the recyclerview (the
            // recyclerview extends behind the status bar) and as such
            // findFirstVisiblePosition() may return the view that is hidden behind the
            //status bar. If we have scrolled so short, then firstVisible should get a ++
            if (scrolled <= topInset) {
                firstVisible++;
            }
        }
        return firstVisible;
    }

    public static void updateNavigationVisibility(List<Comment> comments, LinearLayout scrollNavigation, boolean showNavButtons) {
        if (showNavButtons) {
            //If was gone and shouldn't be now, animate in
            if (comments != null && comments.size() > 1 && scrollNavigation.getVisibility() == View.GONE) {
                scrollNavigation.setVisibility(View.VISIBLE);

                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(400);
                anim.setRepeatMode(Animation.REVERSE);
                scrollNavigation.startAnimation(anim);
            }
        }

    }

    public static void scrollTop(RecyclerView recyclerView) {
        recyclerView.smoothScrollToPosition(0);
    }
    public static void scrollLast(List<Comment> comments, LinearLayoutManager layoutManager, RecyclerView.SmoothScroller smoothScroller) {
        if (layoutManager != null) {
            int firstVisible = layoutManager.findFirstVisibleItemPosition();
            int toScrollTo = firstVisible;

            for (int i = firstVisible + 1; i < comments.size(); i++) {
                if (comments.get(i).depth == 0) {
                    toScrollTo = i;
                }
            }

            smoothScroller.setTargetPosition(toScrollTo);
            layoutManager.startSmoothScroll(smoothScroller);
        }
    }
    public static void scrollPrevious(List<Comment> comments,
                                      LinearLayoutManager layoutManager, RecyclerView.SmoothScroller smoothScroller,
                                      int topInset) {
        if (layoutManager != null) {
            int firstVisible = findFirstVisiblePosition(layoutManager, topInset);

            int toScrollTo = 0;

            for (int i = 0; i < firstVisible; i++) {
                if (comments.get(i).depth == 0 || i == 0) {
                    toScrollTo = i;
                }
            }

            smoothScroller.setTargetPosition(toScrollTo);
            layoutManager.startSmoothScroll(smoothScroller);
        }
    }
    public static void scrollNext(List<Comment> comments, LinearLayoutManager layoutManager, RecyclerView.SmoothScroller smoothScroller, int topInset) {
        if (layoutManager != null) {
            int firstVisible = findFirstVisiblePosition(layoutManager, topInset);

            int toScrollTo = firstVisible;

            for (int i = firstVisible + 1; i < comments.size(); i++) {
                if (comments.get(i).depth == 0) {
                    toScrollTo = i;
                    break;
                }
            }

            smoothScroller.setTargetPosition(toScrollTo);
            layoutManager.startSmoothScroll(smoothScroller);
        }
    }
    public static MaterialAlertDialogBuilder createCommentDialogBuilder(
            Context ctx, Fragment fragment,
            List<Comment> comments, Comment comment,
            LinearLayoutManager layoutManager,
            RecyclerView.SmoothScroller smoothScroller
    ) {
        List<Triplet<String, Integer, CommentAction>> itemsList = CommentsUtils.getPossibleActions(comment);
        ListAdapter adapter = getListAdapter(ctx, itemsList);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ctx);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                var commentAction = itemsList.get(which).third();
                switch (commentAction) {
                    case VIEW_USER ->
                            UserDialogFragment.showUserDialog(fragment.requireActivity().getSupportFragmentManager(), comment.by);
                    case SHARE_COMMENT_LINK ->
                            ctx.startActivity(ShareUtils.getShareIntent(comment.id));
                    case COPY_TEXT -> {
                        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("Hacker News comment", Html.fromHtml(comment.text));
                        clipboard.setPrimaryClip(clip);
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Toast.makeText(ctx, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                        }
                    }
                    case SELECT_TEXT -> DialogUtils.showTextSelectionDialog(ctx, comment.text);
                    case VOTE_UP ->
                            UserActions.upvote(ctx, comment.id, fragment.getParentFragmentManager());
                    case UN_VOTE ->
                            UserActions.unvote(ctx, comment.id, fragment.getParentFragmentManager());
                    case VOTE_DOWN ->
                            UserActions.downvote(ctx, comment.id, fragment.getParentFragmentManager());
                    case BOOKMARK -> {
                        Utils.addBookmark(ctx, comment.id);
                        Snackbar sb = Snackbar.make(ctx, fragment.requireView(), "Comment Bookmarked", Snackbar.LENGTH_SHORT);
                        ViewCompat.setElevation(sb.getView(), Utils.pxFromDp(fragment.getResources(), 24));
                        sb.show();
                    }
                    case PARENT_COMMENT -> {
                        int parentPos = comments.indexOf(comment.parentComment);
                        smoothScroller.setTargetPosition(parentPos);
                        layoutManager.startSmoothScroll(smoothScroller);
                    }
                    case ROOT_COMMENT -> {
                        int rootPos = comments.indexOf(comment.rootComment);
                        smoothScroller.setTargetPosition(rootPos);
                        layoutManager.startSmoothScroll(smoothScroller);
                    }
                    case REPLY -> {
                        if (!AccountUtils.hasAccountDetails(ctx)) {
                            AccountUtils.showLoginPrompt(fragment.getParentFragmentManager());
                            return;
                        }
                        Intent replyIntent = new Intent(ctx, ComposeActivity.class);
                        replyIntent.putExtra(ComposeActivity.EXTRA_ID, comment.id);
                        replyIntent.putExtra(ComposeActivity.EXTRA_PARENT_TEXT, comment.text);
                        replyIntent.putExtra(ComposeActivity.EXTRA_USER, comment.by);
                        replyIntent.putExtra(ComposeActivity.EXTRA_TYPE, ComposeActivity.TYPE_COMMENT_REPLY);
                        ctx.startActivity(replyIntent);
                    }
                }
            }
        });
        return builder;
    }

    @NonNull
    private static ListAdapter getListAdapter(Context ctx, List<Triplet<String, Integer, CommentAction>> itemsList) {
        ListAdapter adapter = new ArrayAdapter<>(ctx,
                R.layout.comment_dialog_item,
                R.id.comment_dialog_text,
                itemsList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);

                view.setCompoundDrawablesWithIntrinsicBounds(itemsList.get(position).second(), 0, 0, 0);
                view.setText((CharSequence) itemsList.get(position).first());

                return view;
            }
        };
        return adapter;
    }


    public static void loadHeaderSpacer(CommentsFragment commentsFragment) {
        if (commentsFragment.recyclerView == null) {
            return;
        }

        RecyclerView.ViewHolder viewHolder = commentsFragment.recyclerView.findViewHolderForAdapterPosition(0);
        if (viewHolder instanceof CommentsRecyclerViewAdapter.HeaderViewHolder) {
            commentsFragment.headerSpacer = ((CommentsRecyclerViewAdapter.HeaderViewHolder) viewHolder).spacer;
        }
    }

    public static void toggleBackPressedCallback(CommentsFragment commentsFragment, boolean newStatus) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            commentsFragment.backPressedCallback.setEnabled(newStatus);
        } else {
            commentsFragment.backPressedCallback.setEnabled(true);
        }
    }

    public static void initializeRecyclerView(final CommentsFragment commentsFragment) {
        commentsFragment.adapter = new CommentsRecyclerViewAdapter(
                commentsFragment.integratedWebview,
                commentsFragment.bottomSheet,
                commentsFragment.requireActivity().getSupportFragmentManager(),
                commentsFragment.comments,
                commentsFragment.story,
                SettingsUtils.shouldCollapseParent(commentsFragment.getContext()),
                SettingsUtils.shouldShowThumbnails(commentsFragment.getContext()),
                commentsFragment.username,
                SettingsUtils.getPreferredCommentTextSize(commentsFragment.getContext()),
                SettingsUtils.shouldUseMonochromeCommentDepthIndicators(commentsFragment.getContext()),
                SettingsUtils.shouldShowNavigationButtons(commentsFragment.getContext()),
                SettingsUtils.getPreferredFont(commentsFragment.getContext()),
                isFeatureSupported(WebViewFeature.FORCE_DARK) || WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING),
                SettingsUtils.shouldShowTopLevelDepthIndicator(commentsFragment.getContext()),
                ThemeUtils.getPreferredTheme(commentsFragment.getContext()),
                Utils.isTablet(commentsFragment.getResources()),
                SettingsUtils.getPreferredFaviconProvider(commentsFragment.getContext()),
                SettingsUtils.shouldSwapCommentLongPressTap(commentsFragment.getContext()));

        commentsFragment.adapter.setOnHeaderClickListener(story1 -> Utils.launchCustomTab(commentsFragment.getActivity(), story1.url));

        commentsFragment.adapter.setOnCommentClickListener((comment, index, commentView) -> {
            comment.expanded = !comment.expanded;

            int offset = 0;
            int lastChildIndex = commentsFragment.adapter.getIndexOfLastChild(comment.depth, index);
            if (index == lastChildIndex && !commentsFragment.adapter.collapseParent) {
                return;
            }

            final RecyclerView.ViewHolder holder = commentsFragment.recyclerView.findViewHolderForAdapterPosition(index);
            if (holder != null && !commentsFragment.adapter.collapseParent && holder instanceof CommentsRecyclerViewAdapter.ItemViewHolder) {
                //if we can reach the ViewHolder (which we should), we can animate the
                // hiddenIndicator ourselves to get around a FULL item refresh (which flashes
                // all the text which we don't want)
                offset = 1;
                final TextView hiddenIndicator = ((CommentsRecyclerViewAdapter.ItemViewHolder) holder).commentHiddenCount;
                int shortAnimationDuration = commentsFragment.getResources().getInteger(android.R.integer.config_shortAnimTime);

                hiddenIndicator.setText("+" + (lastChildIndex - index));

                if (comment.expanded) {
                    //fade out
                    hiddenIndicator.setVisibility(View.VISIBLE);
                    hiddenIndicator.setAlpha(1f);
                    hiddenIndicator.animate()
                            .alpha(0f)
                            .setDuration(shortAnimationDuration)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    hiddenIndicator.setVisibility(View.INVISIBLE);
                                }
                            });
                } else {
                    //fade in
                    hiddenIndicator.setVisibility(View.VISIBLE);
                    hiddenIndicator.setAlpha(0f);
                    hiddenIndicator.animate()
                            .alpha(1f)
                            .setDuration(shortAnimationDuration)
                            .setListener(null);
                }
            } else {
                commentsFragment.adapter.notifyItemChanged(index);
            }

            if (lastChildIndex != index || commentsFragment.adapter.collapseParent) {
                // + 1 since if we have 1 subcomment we have changed the parent and the child
                commentsFragment.adapter.notifyItemRangeChanged(index + 1, lastChildIndex - index + 1 - offset);
            }

            //next couple of lines makes it so that if we hide parents and click the comment at
            //the top of the screen, we scroll down to the next comment automatically
            //this is only applicable if we're hiding a comment
            if (commentsFragment.layoutManager != null && !comment.expanded && commentsFragment.adapter.collapseParent) {
                int firstVisible = commentsFragment.layoutManager.findFirstVisibleItemPosition();
                int clickedIndex = commentsFragment.comments.indexOf(comment);

                //if we clicked the top one and the new top level comment exists
                if (clickedIndex == firstVisible && commentsFragment.comments.size() > lastChildIndex + 1) {
                    commentsFragment.smoothScroller.setTargetPosition(lastChildIndex + 1);
                    commentsFragment.layoutManager.startSmoothScroll(commentsFragment.smoothScroller);

                }
            }
        });

        commentsFragment.adapter.setOnCommentLongClickListener(commentsFragment);
        commentsFragment.adapter.setRetryListener(commentsFragment::refreshComments);

        commentsFragment.adapter.setOnHeaderActionClickListener(new CommentsRecyclerViewAdapter.HeaderActionClickListener() {
            @Override
            @SuppressWarnings("deprecation")
            public void onActionClicked(int flag, View clickedView) {
                switch (flag) {
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_USER -> commentsFragment.clickUser();
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_COMMENT -> commentsFragment.clickComment();
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_VOTE -> commentsFragment.clickVote();
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_PARENT -> commentsFragment.clickParent();
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_SHARE ->
                            commentsFragment.clickShare(clickedView);
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_MORE ->
                            commentsFragment.clickMore(clickedView);
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_REFRESH -> commentsFragment.webView.reload();
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_EXPAND ->
                            BottomSheetBehavior.from(commentsFragment.bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_BROWSER -> commentsFragment.clickBrowser();
                    case CommentsRecyclerViewAdapter.FLAG_ACTION_CLICK_INVERT -> {
                        //this whole thing should only be visible for SDK_INT larger than Q (29)
                        //We first check the "new" version of dark mode, algorithmic darkening
                        // this requires the isDarkMode thing to be true for the theme which we
                        // have set
                        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(commentsFragment.webView.getSettings(), !WebSettingsCompat.isAlgorithmicDarkeningAllowed(commentsFragment.webView.getSettings()));
                        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                            //I don't know why but this seems to always be true whenever we
                            //are at or above android 10
                            if (WebSettingsCompat.getForceDark(commentsFragment.webView.getSettings()) == WebSettingsCompat.FORCE_DARK_ON) {
                                WebSettingsCompat.setForceDark(commentsFragment.webView.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
                            } else {
                                WebSettingsCompat.setForceDark(commentsFragment.webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
                            }
                        }
                    }
                }
            }
        });

        if (commentsFragment.integratedWebview) {
            commentsFragment.recyclerView = commentsFragment.recyclerViewRegular;
        } else {
            commentsFragment.recyclerView = commentsFragment.recyclerViewSwipe;
        }

        commentsFragment.layoutManager = new LinearLayoutManager(commentsFragment.getContext());

        commentsFragment.recyclerView.setLayoutManager(commentsFragment.layoutManager);
        commentsFragment.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (commentsFragment.integratedWebview) {
                    //Shouldn't be necessary but once I was stuck in comments and couldn't swipe up.
                    //this just updates a flag so there's no performance impact
                    if (dy != 0 && commentsFragment.callback != null) {
                        commentsFragment.callback.onSwitchView(false);
                    }
                    BottomSheetBehavior.from(commentsFragment.bottomSheet).setDraggable(recyclerView.computeVerticalScrollOffset() == 0);
                }
            }
        });
        commentsFragment.smoothScroller = new LinearSmoothScroller(commentsFragment.requireContext()) {
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return commentsFragment.layoutManager.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int getVerticalSnapPreference() {
                return LinearSmoothScroller.SNAP_TO_START;
            }

            @Override
            public int calculateDyToMakeVisible(View view, int snapPreference) {
                //this is to make sure that scrollTo calls work properly
                return super.calculateDyToMakeVisible(view, snapPreference) + commentsFragment.topInset;
            }

        };

        if (!SettingsUtils.shouldUseCommentsAnimation(commentsFragment.getContext())) {
            commentsFragment.recyclerView.setItemAnimator(null);
        }

        if (!SettingsUtils.shouldUseCommentsScrollbar(commentsFragment.getContext())) {
            //for some reason, I could only get the scrollbars to show up when they are enabled via
            //xml but disabling them in java worked so this is an okay solution...
            commentsFragment.recyclerView.setVerticalScrollBarEnabled(false);
        }

        BottomSheetBehavior.from(commentsFragment.bottomSheet).addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    toggleBackPressedCallback(commentsFragment, commentsFragment.webView != null && commentsFragment.webView.canGoBack());
                } else {
                    toggleBackPressedCallback(commentsFragment, false);
                }
            }

            @Override
            public void onSlide(@NonNull View view, float slideOffset) {
                // Updating padding (of recyclerview) doesn't work because it causes incorrect scroll position for recycler.
                // Updating scroll together with padding causes severe lags and other problems.
                // So don't update padding at all on slide and instead just change whole view position (by translationY on recyclerView)
                //... is something you could do but this means that the touch target of the recyclerview is not aligned with the view
                //so we go back to the padding but instead just put a view above the recyclerview (a spacer) and change its height!
                //... is what you could do if you were stupid! This would mean that the recyclerView starts BELOW the status bar
                //breaking transparent status bar. Instead, the spacing needs to be _within_ the recyclerview header!
                //NOTE: this also needs to be set in onBindViewHolder of the adapter to stay up to date if the header item
                //should be refreshed
                loadHeaderSpacer(commentsFragment);
                if (commentsFragment.headerSpacer != null) {
                    commentsFragment.headerSpacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.round(commentsFragment.topInset * slideOffset)));
                    commentsFragment.adapter.spacerHeight = Math.round(commentsFragment.topInset * slideOffset);
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(commentsFragment.recyclerView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                commentsFragment.topInset = insets.top;

                float offset = BottomSheetBehavior.from(commentsFragment.bottomSheet).calculateSlideOffset();

                loadHeaderSpacer(commentsFragment);
                if (commentsFragment.headerSpacer != null) {
                    commentsFragment.headerSpacer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.round(commentsFragment.topInset * offset)));
                    commentsFragment.adapter.spacerHeight = Math.round(commentsFragment.topInset * offset);
                }

                int paddingBottom = insets.bottom + commentsFragment.getResources().getDimensionPixelSize(commentsFragment.showNavButtons ? R.dimen.comments_bottom_navigation : R.dimen.comments_bottom_standard);
                commentsFragment.recyclerView.setPadding(commentsFragment.recyclerView.getPaddingLeft(), commentsFragment.recyclerView.getPaddingTop(), commentsFragment.recyclerView.getPaddingRight(), paddingBottom);

                return windowInsets;
            }
        });
        com.simon.harmonichackernews.utils.ViewUtils.requestApplyInsetsWhenAttached(commentsFragment.recyclerView);

        commentsFragment.recyclerView.setAdapter(commentsFragment.adapter);

        commentsFragment.recyclerView.getRecycledViewPool().setMaxRecycledViews(CommentsRecyclerViewAdapter.TYPE_COMMENT, 300);
        commentsFragment.recyclerView.getRecycledViewPool().setMaxRecycledViews(CommentsRecyclerViewAdapter.TYPE_COLLAPSED, 600);
        commentsFragment.recyclerView.getRecycledViewPool().setMaxRecycledViews(CommentsRecyclerViewAdapter.TYPE_HEADER, 1);
    }

    public static void showDownloadButton(final CommentsFragment commentsFragment, String url, String contentDisposition, String mimetype) {
        if (commentsFragment.webView != null && commentsFragment.downloadButton != null) {
            commentsFragment.webView.setVisibility(View.GONE);
            commentsFragment.downloadButton.setVisibility(View.VISIBLE);
            commentsFragment.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //just download via notification as usual
                    try {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
                        DownloadManager dm = (DownloadManager) view.getContext().getSystemService(DOWNLOAD_SERVICE);
                        dm.enqueue(request);
                        Toast.makeText(commentsFragment.getContext(), "Downloading...", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(commentsFragment.getContext(), "Failed to download, opening in browser", Toast.LENGTH_LONG).show();
                        Utils.launchInExternalBrowser(commentsFragment.getActivity(), url);
                    }

                }
            });
        }
    }

    @SuppressLint({"RequiresFeature", "SetJavaScriptEnabled"})
    @SuppressWarnings("deprecation")
    public static void initializeWebView(final CommentsFragment commentsFragment) {
        commentsFragment.initializedWebView = true;
        BottomSheetBehavior.from(commentsFragment.bottomSheet).setDraggable(true);
        BottomSheetBehavior.from(commentsFragment.bottomSheet).addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (commentsFragment.callback != null) {
                    commentsFragment.callback.onSwitchView(newState == BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //onSlide gets called when if we're just scrolling the scrollview in the sheet,
                //we only want to start loading if we're actually sliding up the thing!
                if (!commentsFragment.startedLoading && slideOffset < 0.9999) {
                    commentsFragment.startedLoading = true;
                    CommentsUtils.loadUrl(commentsFragment, commentsFragment.story.url);
                }
            }
        });

        //This is because we are now for sure not using swipeRefresh
        try {
            ((FrameLayout) commentsFragment.swipeRefreshLayout.getParent()).removeView(commentsFragment.swipeRefreshLayout);
        } catch (Exception e) {
            //this will crash if we have already done this, which is fine
        }

        if (commentsFragment.blockAds && TextUtils.isEmpty(Utils.adservers)) {
            Utils.loadAdservers(commentsFragment.getResources());
        }

        commentsFragment.webView.setWebViewClient(commentsFragment.new MyWebViewClient());
        if (commentsFragment.preloadWebview.equals("always") || (commentsFragment.preloadWebview.equals("onlywifi") && Utils.isOnWiFi(commentsFragment.requireContext())) || commentsFragment.showWebsite || (NitterGetter.isConvertibleToNitter(commentsFragment.story.url) && SettingsUtils.shouldUseLinkPreviewX(commentsFragment.getContext()))) {
            CommentsUtils.loadUrl(commentsFragment, commentsFragment.story.url);
            commentsFragment.startedLoading = true;
        }

        commentsFragment.webView.getSettings().setBuiltInZoomControls(true);
        commentsFragment.webView.getSettings().setDisplayZoomControls(false);
        commentsFragment.webView.getSettings().setJavaScriptEnabled(true);
        commentsFragment.webView.getSettings().setDomStorageEnabled(true);
        commentsFragment.webView.getSettings().setGeolocationEnabled(true);
        commentsFragment.webView.getSettings().setDatabaseEnabled(true);
        commentsFragment.webView.getSettings().setUseWideViewPort(true);
        commentsFragment.webView.getSettings().setLoadWithOverviewMode(true);

        commentsFragment.webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

                if (!TextUtils.isEmpty(mimetype) && mimetype.equals(CommentsFragment.PDF_MIME_TYPE) && (url.startsWith("http://") || url.startsWith("https://"))) {
                    CommentsUtils.downloadPdf(commentsFragment, url, contentDisposition, mimetype, commentsFragment.webView.getContext());
                } else {
                    showDownloadButton(commentsFragment, url, contentDisposition, mimetype);
                }
            }
        });

        commentsFragment.webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100 && commentsFragment.progressIndicator.getVisibility() == View.GONE) {
                    commentsFragment.progressIndicator.setVisibility(View.VISIBLE);
                }

                commentsFragment.progressIndicator.setProgress(progress);
                if (progress == 100) {
                    commentsFragment.progressIndicator.setVisibility(View.GONE);
                }
            }
        });

        if (commentsFragment.matchWebviewTheme && ThemeUtils.isDarkMode(commentsFragment.getContext())) {
            if (isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(commentsFragment.webView.getSettings(), true);
            } else if (isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(commentsFragment.webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            }
        }

        commentsFragment.webView.setBackgroundColor(Color.TRANSPARENT);

        commentsFragment.webViewBackdrop.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (commentsFragment.webViewBackdrop != null) {
                    commentsFragment.webViewBackdrop.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                }

            }
        }, 2000); // Start the animation after 2 seconds
    }

    public static void restoreScrollProgress(CommentsFragment commentsFragment, CommentsScrollProgress scrollProgress) {
        for (Comment c : commentsFragment.comments) {
            if (c.id == scrollProgress.topCommentId) {
                commentsFragment.layoutManager.scrollToPositionWithOffset(commentsFragment.comments.indexOf(c), scrollProgress.topCommentOffset);
            }
            c.expanded = !scrollProgress.collapsedIDs.contains(c.id);
        }
    }
}
