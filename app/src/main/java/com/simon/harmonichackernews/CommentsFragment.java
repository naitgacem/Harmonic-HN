package com.simon.harmonichackernews;

import static com.simon.harmonichackernews.adapters.CommentsRecyclerViewAdapter.TYPE_COLLAPSED;
import static com.simon.harmonichackernews.view.ViewUtils.createCommentDialogBuilder;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.simon.harmonichackernews.adapters.CommentsRecyclerViewAdapter;
import com.simon.harmonichackernews.data.Comment;
import com.simon.harmonichackernews.data.CommentsScrollProgress;
import com.simon.harmonichackernews.data.NitterInfo;
import com.simon.harmonichackernews.data.Story;
import com.simon.harmonichackernews.linkpreview.NitterGetter;
import com.simon.harmonichackernews.network.ArchiveOrgUrlGetter;
import com.simon.harmonichackernews.network.NetworkComponent;
import com.simon.harmonichackernews.network.UserActions;
import com.simon.harmonichackernews.utils.AccountUtils;
import com.simon.harmonichackernews.utils.CommentsUtils;
import com.simon.harmonichackernews.utils.SettingsUtils;
import com.simon.harmonichackernews.utils.ShareUtils;
import com.simon.harmonichackernews.utils.ThemeUtils;
import com.simon.harmonichackernews.utils.Utils;
import com.simon.harmonichackernews.utils.ViewUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CommentsFragment extends Fragment implements CommentsRecyclerViewAdapter.CommentClickListener {

    public final static String PDF_MIME_TYPE = "application/pdf";
    public final static String PDF_LOADER_URL = "file:///android_asset/pdf/index.html";
    public final Object requestTag = new Object();
    public BottomSheetFragmentCallback callback;
    public List<Comment> comments;
    public RequestQueue queue;
    public CommentsRecyclerViewAdapter adapter;
    public SwipeRefreshLayout swipeRefreshLayout;
    public RecyclerView recyclerView;
    public RecyclerView recyclerViewSwipe;
    public RecyclerView recyclerViewRegular;
    public LinearLayoutManager layoutManager;
    public RecyclerView.SmoothScroller smoothScroller;
    public LinearLayout scrollNavigation;
    public LinearProgressIndicator progressIndicator;
    public LinearLayout bottomSheet;
    public WebView webView;
    public FrameLayout webViewContainer;
    public View webViewBackdrop;
    public Space headerSpacer;
    public MaterialButton downloadButton;
    public boolean showNavButtons = false;
    public boolean showWebsite = false;
    public boolean integratedWebview = true;
    public boolean prefIntegratedWebview = true;
    public String preloadWebview = "never";
    public boolean matchWebviewTheme = true;
    public boolean blockAds = true;
    public boolean startedLoading = false;
    public boolean initializedWebView = false;
    public int topInset = 0;
    public long lastLoaded = 0;
    public OnBackPressedCallback backPressedCallback;
    public String username;
    public Story story;
    public CommentsFragment() {
        super(R.layout.fragment_comments);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, true));
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, false));

        story = new Story();

        Bundle bundle = getArguments();
        if (bundle != null && bundle.getString(CommentsUtils.EXTRA_TITLE) != null && bundle.getString(CommentsUtils.EXTRA_BY) != null) {
            CommentsUtils.fillStoryFromBundle(story, bundle);

            if (Utils.isTablet(getResources())) {
                int forward = bundle.getInt(CommentsUtils.EXTRA_FORWARD, 0);
                if (forward == 0) {
                    setExitTransition(new MaterialFadeThrough());
                    setEnterTransition(new MaterialFadeThrough());
                } else if (forward > 0) {
                    setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, true));
                    setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, false));
                } else {
                    setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, false));
                    setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Y, true));
                }
            }

            showWebsite = bundle.getBoolean(CommentsUtils.EXTRA_SHOW_WEBSITE, false);
        } else {
            story.loaded = false;
            story.id = -1;
            //check if url intercept
            Intent intent = requireActivity().getIntent();
            if (intent == null) {
                return;
            }

            if (Intent.ACTION_VIEW.equalsIgnoreCase(intent.getAction())) {
                if (intent.getData() != null) {
                    String sId = intent.getData().getQueryParameter("id");
                    String sFragmentId = intent.getData().getFragment();
                    try {
                        int id = Integer.parseInt(sId);
                        if (id > 0) {
                            Utils.initStory(story, id);
                        }

                        // Check if there is a fragment that should replace the story id
                        try {
                            id = Integer.parseInt(sFragmentId);
                            if (id > 0) {
                                Utils.initStory(story, id);
                            }
                        } catch (Exception ignored) {
                            // we tried..
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Unable to parse story", Toast.LENGTH_SHORT).show();
                        requireActivity().finish();
                    }
                }
                if (story.id == -1) {
                    Toast.makeText(getContext(), "Unable to parse story", Toast.LENGTH_SHORT).show();
                    requireActivity().finish();
                }
            } else {
                if (intent.getIntExtra(CommentsUtils.EXTRA_ID, -1) != -1) {
                    story.id = intent.getIntExtra(CommentsUtils.EXTRA_ID, -1);
                    story.parentId = intent.getIntExtra(CommentsUtils.EXTRA_PARENT_ID, 0);
                    story.title = intent.getStringExtra(CommentsUtils.EXTRA_TITLE);
                    story.by = "";
                    story.url = "";
                    story.score = 0;
                }
            }

        }


    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof BottomSheetFragmentCallback) {
            callback = (BottomSheetFragmentCallback) getActivity();
        }

        prefIntegratedWebview = SettingsUtils.shouldUseIntegratedWebView(getContext());

        integratedWebview = prefIntegratedWebview && story.isLink;
        preloadWebview = SettingsUtils.shouldPreloadWebView(getContext());
        matchWebviewTheme = SettingsUtils.shouldMatchWebViewTheme(getContext());
        blockAds = SettingsUtils.shouldBlockAds(getContext());

        webView = view.findViewById(R.id.comments_webview);
        downloadButton = view.findViewById(R.id.webview_download);
        swipeRefreshLayout = view.findViewById(R.id.comments_swipe_refresh);
        recyclerViewRegular = view.findViewById(R.id.comments_recyclerview);
        recyclerViewSwipe = view.findViewById(R.id.comments_recyclerview_swipe);
        bottomSheet = view.findViewById(R.id.comments_bottom_sheet);
        webViewContainer = view.findViewById(R.id.webview_container);
        webViewBackdrop = view.findViewById(R.id.comments_webview_backdrop);

        if (story.title == null) {
            //Empty view for tablets
            view.findViewById(R.id.comments_empty).setVisibility(View.VISIBLE);
            bottomSheet.setVisibility(View.GONE);
            webViewContainer.setVisibility(View.GONE);

            swipeRefreshLayout.setEnabled(false);
            return;
        }

        backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (BottomSheetBehavior.from(bottomSheet).getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (webView.canGoBack()) {
                        if (downloadButton.getVisibility() == View.VISIBLE && webView.getVisibility() == View.GONE) {
                            webView.setVisibility(View.VISIBLE);
                            downloadButton.setVisibility(View.GONE);
                        } else {
                            webView.goBack();
                        }
                        return;
                    }
                }
                requireActivity().finish();
                if (!SettingsUtils.shouldDisableCommentsSwipeBack(getContext()) && !Utils.isTablet(getResources())) {
                    requireActivity().overridePendingTransition(0, R.anim.activity_out_animation);
                }
            }
        };
        com.simon.harmonichackernews.view.ViewUtils.toggleBackPressedCallback(this, false);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);

        swipeRefreshLayout.setOnRefreshListener(this::refreshComments);
        ViewUtils.setUpSwipeRefreshWithStatusBarOffset(swipeRefreshLayout);

        // this is how much the bottom sheet sticks up by default and also decides height of webview
        //We want to watch for navigation bar height changes (tablets on Android 12L can cause
        // these)

        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                updateBottomSheetMargin(insets.bottom);

                webViewContainer.setPadding(0, insets.top, 0, 0);

                return windowInsets;
            }
        });
        ViewUtils.requestApplyInsetsWhenAttached(view);

        if (!showWebsite) {
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        if (callback != null) {
            callback.onSwitchView(showWebsite);
        }

        if (integratedWebview) {
            swipeRefreshLayout.setEnabled(false);
            swipeRefreshLayout.setNestedScrollingEnabled(true);
        }

        progressIndicator = view.findViewById(R.id.webview_progress);

        if (integratedWebview) {
            com.simon.harmonichackernews.view.ViewUtils.initializeWebView(this);
        } else {
            BottomSheetBehavior.from(bottomSheet).setDraggable(false);
        }

        bottomSheet.setBackgroundColor(ContextCompat.getColor(requireContext(), ThemeUtils.getBackgroundColorResource(requireContext())));
        webViewContainer.setBackgroundColor(ContextCompat.getColor(requireContext(), ThemeUtils.getBackgroundColorResource(requireContext())));

        comments = new ArrayList<>();
        comments.add(new Comment()); //header

        username = AccountUtils.getAccountUsername(getContext());

        scrollNavigation = view.findViewById(R.id.comments_scroll_navigation);
        ViewCompat.setOnApplyWindowInsetsListener(scrollNavigation, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());

                FrameLayout.LayoutParams scrollParams = (FrameLayout.LayoutParams) scrollNavigation.getLayoutParams();
                scrollParams.setMargins(0, 0, 0, insets.bottom + Utils.pxFromDpInt(getResources(), 16));

                return windowInsets;
            }
        });
        ViewUtils.requestApplyInsetsWhenAttached(scrollNavigation);

        showNavButtons = SettingsUtils.shouldShowNavigationButtons(getContext());
        com.simon.harmonichackernews.view.ViewUtils.updateNavigationVisibility(comments, scrollNavigation, showNavButtons);

        ImageButton scrollPrev = view.findViewById(R.id.comments_scroll_previous);
        ImageButton scrollNext = view.findViewById(R.id.comments_scroll_next);
        ImageView scrollIcon = view.findViewById(R.id.comments_scroll_icon);

        scrollIcon.setOnClickListener(null);

        scrollNext.setOnClickListener((v) -> com.simon.harmonichackernews.view.ViewUtils.scrollNext(comments, layoutManager, smoothScroller, topInset));
        scrollNext.setOnLongClickListener(v -> {
            com.simon.harmonichackernews.view.ViewUtils.scrollLast(comments, layoutManager, smoothScroller);
            return true;
        });

        scrollPrev.setOnClickListener((v) -> com.simon.harmonichackernews.view.ViewUtils.scrollPrevious(comments, layoutManager, smoothScroller, topInset));
        scrollPrev.setOnLongClickListener(v -> {
            com.simon.harmonichackernews.view.ViewUtils.scrollTop(recyclerView);
            return true;
        });

        com.simon.harmonichackernews.view.ViewUtils.initializeRecyclerView(this);

        queue = NetworkComponent.getRequestQueueInstance(requireContext());
        String cachedResponse = Utils.loadCachedStory(getContext(), story.id);

        CommentsUtils.loadStoryAndComments(this, story.id, cachedResponse);

        //if this isn't here, the addition of the text appears to scroll the recyclerview down a little
        recyclerView.scrollToPosition(0);


        if (cachedResponse != null) {
            CommentsUtils.handleJsonResponse(this, story.id, cachedResponse, false, false, !showWebsite);
        }
    }

    private void updateBottomSheetMargin(int navbarHeight) {
        int standardMargin = Utils.pxFromDpInt(getResources(), Utils.isTablet(getResources()) ? 81 : 68);

        BottomSheetBehavior.from(bottomSheet).setPeekHeight(standardMargin + navbarHeight);
        CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.setMargins(0, 0, 0, standardMargin + navbarHeight);

        webViewContainer.setLayoutParams(params);

        if (adapter != null) {
            adapter.setNavbarHeight(navbarHeight);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //this is to make sure that action buttons in header get updated padding on rotations...
        //yes its ugly, I know
        if (getContext() != null && Utils.isTablet(getResources()) && adapter != null) {
            adapter.notifyItemChanged(0);
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        if (callback == null && getActivity() instanceof BottomSheetFragmentCallback) {
            callback = (BottomSheetFragmentCallback) getActivity();
        }

        if (callback != null) {
            callback.onSwitchView(BottomSheetBehavior.from(bottomSheet).getState() == BottomSheetBehavior.STATE_COLLAPSED);
        }

        CommentsUtils.initAdapter(this, adapter, bottomSheet, comments, webViewContainer);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (lastLoaded != 0 && (System.currentTimeMillis() - lastLoaded) > 1000 * 60 * 60 && !Utils.timeInSecondsMoreThanTwoHoursAgo(story.time)) {
            if (adapter != null && !adapter.showUpdate) {
                adapter.showUpdate = true;
                adapter.notifyItemChanged(0);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (layoutManager != null) {
            if (layoutManager.findFirstVisibleItemPosition() != RecyclerView.NO_POSITION) {
                if (MainActivity.commentsScrollProgresses == null) {
                    MainActivity.commentsScrollProgresses = new ArrayList<>();
                }
                //let's check all scrollProgresses in memory to see if we should change an active
                //object
                for (int i = 0; i < MainActivity.commentsScrollProgresses.size(); i++) {
                    CommentsScrollProgress scrollProgress = MainActivity.commentsScrollProgresses.get(i);
                    if (scrollProgress.storyId == story.id) {
                        // if we find, overwrite the old thing and stop completely
                        MainActivity.commentsScrollProgresses.set(i, recordScrollProgress());
                        return;
                    }
                }

                //if we didn't find anything, let's add it ourselves
                MainActivity.commentsScrollProgresses.add(recordScrollProgress());
            }
        }
    }

    private CommentsScrollProgress recordScrollProgress() {
        CommentsScrollProgress scrollProgress = new CommentsScrollProgress();

        int lastScrollIndex = layoutManager.findFirstVisibleItemPosition();
        scrollProgress.storyId = story.id;
        scrollProgress.topCommentId = comments.get(lastScrollIndex).id;

        scrollProgress.collapsedIDs = new HashSet<>();

        for (Comment c : comments) {
            if (!c.expanded) {
                scrollProgress.collapsedIDs.add(c.id);
            }
        }

        View firstVisibleItem = recyclerView.getChildAt(0);
        scrollProgress.topCommentOffset = (firstVisibleItem == null) ? 0 : (firstVisibleItem.getTop() - recyclerView.getPaddingTop());

        return scrollProgress;
    }

    public void restartWebView() {
        CommentsUtils.destroyWebView(webViewContainer, webView);

        webView = new WebView(getContext());
        webViewContainer.addView(webView);
        com.simon.harmonichackernews.view.ViewUtils.initializeWebView(this);
    }

    @Override
    public void onDestroyView() {
        if (recyclerView != null) {
            recyclerView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                    // no-op
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                    recyclerView.setAdapter(null);
                }
            });
        }

        super.onDestroyView();

        if (queue != null) {
            queue.cancelAll(requestTag);
        }
        CommentsUtils.destroyWebView(webViewContainer, webView);
    }

    public void refreshComments() {
        swipeRefreshLayout.setRefreshing(true);
        CommentsUtils.loadStoryAndComments(this, adapter.story.id, null);
    }

    public void clickBrowser() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        try {
            intent.setData(Uri.parse(webView.getUrl()));
            startActivity(intent);
        } catch (Exception e) {
            //if we're at a PDF or something like that, just do the original URL
            intent.setData(Uri.parse(story.url));
            startActivity(intent);
        }

    }

    public void clickShare(View view) {
        PopupMenu popup = new PopupMenu(requireActivity(), view);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.menu_link) {
                    startActivity(ShareUtils.getShareIntent(adapter.story.url));
                } else if (itemId == R.id.menu_link_title) {
                    startActivity(ShareUtils.getShareIntentWithTitle(adapter.story.title, adapter.story.url));
                } else if (itemId == R.id.menu_hacker_news_link) {
                    startActivity(ShareUtils.getShareIntent(adapter.story.id));
                } else if (itemId == R.id.menu_hacker_news_link_title) {
                    startActivity(ShareUtils.getShareIntentWithTitle(adapter.story.title, adapter.story.id));
                }

                return true;
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.share_menu, popup.getMenu());

        if (!adapter.story.isLink) {
            popup.getMenu().findItem(R.id.menu_link).setVisible(false);
            popup.getMenu().findItem(R.id.menu_link_title).setVisible(false);
        }

        popup.show();
    }

    public void clickMore(View view) {
        PopupMenu popup = new PopupMenu(requireActivity(), view);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.menu_refresh) {
                    refreshComments();
                } else if (id == R.id.menu_adblock) {
                    blockAds = false;
                    webView.reload();

                    Snackbar snackbar = Snackbar.make(webView, "Disabled AdBlock, refreshing WebView", Snackbar.LENGTH_SHORT);
                    ViewCompat.setElevation(snackbar.getView(), Utils.pxFromDp(getResources(), 24));
                    snackbar.show();
                } else if (id == R.id.menu_archive) {
                    Toast.makeText(getContext(), "Contacting archive.org API...", Toast.LENGTH_SHORT).show();
                    ArchiveOrgUrlGetter.getArchiveUrl(story.url, getContext(), new ArchiveOrgUrlGetter.GetterCallback() {
                        @Override
                        public void onSuccess(String url) {
                            Utils.launchCustomTab(getActivity(), url);
                        }

                        @Override
                        public void onFailure(String reason) {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error: " + reason, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else if (id == R.id.menu_search_comments) {
                    CommentsSearchDialogFragment.showCommentSearchDialog(getParentFragmentManager(), comments, new CommentsSearchDialogFragment.CommentSelectedListener() {
                        @Override
                        public void onCommentSelected(Comment comment) {
                            for (Comment c : comments) {
                                if (c.id != comment.id) {
                                    continue;
                                }

                                int position = comments.indexOf(c);
                                Comment rootComment = c;
                                if (adapter.getItemViewType(position) == TYPE_COLLAPSED && c.rootComment != null) {
                                    rootComment = c.rootComment;
                                }
                                smoothScroller.setTargetPosition(position);
                                layoutManager.startSmoothScroll(smoothScroller);


                                if (!rootComment.expanded) {
                                    rootComment.expanded = true;
                                    adapter.notifyItemRangeChanged(position, rootComment.children);
                                }
                                break;
                            }
                        }
                    });
                }

                return true;
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.comments_more_menu, popup.getMenu());

        for (int i = 0; i < popup.getMenu().size(); i++) {
            MenuItem item = popup.getMenu().getItem(i);

            if (!story.isLink && item.getItemId() == R.id.menu_archive) {
                item.setVisible(false);
            }

            if (!SettingsUtils.shouldBlockAds(getContext()) && item.getItemId() == R.id.menu_adblock) {
                item.setVisible(false);
            }

            if (item.getItemId() == R.id.menu_search_comments && comments.size() < 2) {
                item.setVisible(false);
            }
        }

        popup.show();
    }

    public void clickUser() {
        UserDialogFragment.showUserDialog(requireActivity().getSupportFragmentManager(), adapter.story.by);
    }

    public void clickComment() {
        if (!AccountUtils.hasAccountDetails(getContext())) {
            AccountUtils.showLoginPrompt(getParentFragmentManager());
            return;
        }

        Intent intent = new Intent(getContext(), ComposeActivity.class);
        intent.putExtra(ComposeActivity.EXTRA_ID, adapter.story.id);
        intent.putExtra(ComposeActivity.EXTRA_PARENT_TEXT, adapter.story.title);
        intent.putExtra(ComposeActivity.EXTRA_TYPE, ComposeActivity.TYPE_TOP_COMMENT);
        startActivity(intent);
    }

    public void clickVote() {
        UserActions.upvote(getContext(), adapter.story.id, getParentFragmentManager());
    }

    public void clickParent() {
        if (story.parentId == 0) {
            Toast.makeText(requireContext(), "This is the top level story.", Toast.LENGTH_SHORT).show();
            return;
        }
        Story parent = new Story("", story.parentId, false, false);
        Bundle bundle = parent.toBundle();
        Intent intent = new Intent(getContext(), CommentsActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void onItemClick(Comment comment, int pos, View view) {
        final Context ctx = getContext();
        if (ctx == null) {
            return;
        }

        MaterialAlertDialogBuilder builder = createCommentDialogBuilder(
                ctx, this, comments,
                comment, layoutManager, smoothScroller);

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    public interface BottomSheetFragmentCallback {
        void onSwitchView(boolean isAtWebView);
    }

    // TODO move this class out of here
    public class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            webView.setBackgroundColor(Color.WHITE);
            webViewBackdrop.setVisibility(View.GONE);

            if (BottomSheetBehavior.from(bottomSheet).getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                //if we are at the webview and we just loaded, recheck the canGoBack status
                com.simon.harmonichackernews.view.ViewUtils.toggleBackPressedCallback(CommentsFragment.this, webView != null && webView.canGoBack());
            }


            if (NitterGetter.isValidNitterUrl(url) && SettingsUtils.shouldUseLinkPreviewX(getContext())) {
                NitterGetter.getInfo(view, getContext(), new NitterGetter.GetterCallback() {
                    @Override
                    public void onSuccess(NitterInfo nitterInfo) {
                        story.nitterInfo = nitterInfo;
                        if (adapter != null) {
                            adapter.notifyItemChanged(0);
                        }
                    }

                    @Override
                    public void onFailure(String reason) {

                    }
                });
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("intent://")) {
                try {
                    Context context = view.getContext();
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                    // First, try to use the fallback URL (browser version of Play Store)
                    String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                    if (fallbackUrl != null) {
                        webView.loadUrl(fallbackUrl);
                        return true; // Indicate that we're handling this URL
                    } else {
                        // If no valid fallback URL, then check if the intent can be resolved (Play Store app is installed)
                        if (intent.resolveActivity(context.getPackageManager()) != null) {
                            context.startActivity(intent);
                            return true; // Indicate that we're handling this URL
                        }
                    }
                } catch (Exception e) {
                    // Handle the error
                    return false; // Indicate that we're not handling this URL
                }
            }

            return false;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (!blockAds) {
                return super.shouldInterceptRequest(view, request);
            }
            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
            if (!TextUtils.isEmpty(Utils.adservers)) {
                if (Utils.adservers.contains(":::::" + request.getUrl().getHost())) {
                    Utils.log("Blocked: " + request.getUrl());
                    return new WebResourceResponse("text/plain", "utf-8", EMPTY);
                }
            }

            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!detail.didCrash()) {
                    // Renderer is killed because the system ran out of memory. The app
                    // can recover gracefully by creating a new WebView instance in the
                    // foreground.
                    Log.e("MY_APP_TAG", "System killed the WebView rendering process " +
                            "to reclaim memory. Recreating...");

                    Utils.toast("System ran out of memory and killed WebView, reinitializing", getContext());
                    restartWebView();

                    // By this point, the instance variable "mWebView" is guaranteed to
                    // be null, so it's safe to reinitialize it.

                    return true; // The app continues executing.
                }
            }
            Utils.toast("WebView crashed, reinitializing", getContext());
            restartWebView();

            // Renderer crashes because of an internal error, such as a memory
            // access violation.
            Log.e("MY_APP_TAG", "The WebView rendering process crashed!");

            // In this example, the app itself crashes after detecting that the
            // renderer crashed. If you handle the crash more gracefully and let
            // your app continue executing, you must destroy the current WebView
            // instance, specify logic for how the app continues executing, and
            // return "true" instead.
            return true;
        }
    }

}
