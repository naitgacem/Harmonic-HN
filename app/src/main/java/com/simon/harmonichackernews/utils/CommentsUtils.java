package com.simon.harmonichackernews.utils;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.simon.harmonichackernews.CommentsFragment;
import com.simon.harmonichackernews.MainActivity;
import com.simon.harmonichackernews.R;
import com.simon.harmonichackernews.adapters.CommentsRecyclerViewAdapter;
import com.simon.harmonichackernews.data.ArxivInfo;
import com.simon.harmonichackernews.data.Comment;
import com.simon.harmonichackernews.data.CommentsScrollProgress;
import com.simon.harmonichackernews.data.PollOption;
import com.simon.harmonichackernews.data.RepoInfo;
import com.simon.harmonichackernews.data.Story;
import com.simon.harmonichackernews.data.WikipediaInfo;
import com.simon.harmonichackernews.linkpreview.ArxivAbstractGetter;
import com.simon.harmonichackernews.linkpreview.GitHubInfoGetter;
import com.simon.harmonichackernews.linkpreview.NitterGetter;
import com.simon.harmonichackernews.linkpreview.WikipediaGetter;
import com.simon.harmonichackernews.network.JSONParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;

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

    public static void loadUrl(CommentsFragment commentsFragment, String url, @Nullable String pdfFilePath) {
        if (commentsFragment.webView == null) {
            return;
        }
        if (url.equals(CommentsFragment.PDF_LOADER_URL)) {
            PdfAndroidJavascriptBridge pdfAndroidJavascriptBridge = new PdfAndroidJavascriptBridge(pdfFilePath, new PdfAndroidJavascriptBridge.Callbacks() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onLoad() {

                }
            });

            commentsFragment.webView.addJavascriptInterface(pdfAndroidJavascriptBridge, "PdfAndroidJavascriptBridge");
            commentsFragment.webView.setInitialScale(100);
            commentsFragment.webView.getSettings().setLoadWithOverviewMode(true);
            commentsFragment.webView.getSettings().setUseWideViewPort(true);
        }

        if (NitterGetter.isConvertibleToNitter(url) && SettingsUtils.shouldRedirectNitter(commentsFragment.getContext())) {
            url = NitterGetter.convertToNitterUrl(url);
        }

        commentsFragment.webView.loadUrl(url);
    }

    public static void loadUrl(CommentsFragment commentsFragment, String url) {
        loadUrl(commentsFragment, url, null);
    }

    public static void downloadPdf(final CommentsFragment commentsFragment, String url, String contentDisposition, String mimetype, Context ctx) {
        if (ctx == null) {
            return;
        }
        FileDownloader fileDownloader = new FileDownloader(ctx);
        Toast.makeText(ctx, "Loading PDF...", Toast.LENGTH_LONG).show();
        fileDownloader.downloadFile(url, CommentsFragment.PDF_MIME_TYPE, new FileDownloader.FileDownloaderCallback() {
            @Override
            public void onFailure(Call call, IOException e) {
                com.simon.harmonichackernews.view.ViewUtils.showDownloadButton(commentsFragment, url, contentDisposition, mimetype);
            }

            @Override
            public void onSuccess(String filePath) {
                loadUrl(commentsFragment, CommentsFragment.PDF_LOADER_URL, filePath);
            }
        });

    }

    public static void handleJsonResponse(CommentsFragment commentsFragment, final int id, final String response, final boolean cache, final boolean forceHeaderRefresh, boolean restoreScroll) {
        int oldCommentCount = commentsFragment.comments.size();

        // This is what we get if the Algolia API has not indexed the post,
        // we should attempt to show the user an option to switch API:s in this
        // server error case
        if (response.equals(JSONParser.ALGOLIA_ERROR_STRING)) {
            commentsFragment.adapter.loadingFailed = true;
            commentsFragment.adapter.loadingFailedServerError = true;
            commentsFragment.adapter.notifyItemChanged(0);
            commentsFragment.swipeRefreshLayout.setRefreshing(false);
        }

        try {
            JSONObject jsonObject = new JSONObject(response);
            commentsFragment.story.parentId = jsonObject.optInt("parent_id");
            JSONArray children = jsonObject.getJSONArray("children");

            //we run the defauly sorting
            boolean addedNewComment = false;
            for (int i = 0; i < children.length(); i++) {
                boolean added = JSONParser.readChildAndParseSubchilds(children.getJSONObject(i), commentsFragment.comments, commentsFragment.adapter, 0, commentsFragment.story.kids);
                if (added) {
                    addedNewComment = true;
                }
            }
            //if non default, do full refresh after the sorting below!
            if (addedNewComment && !SettingsUtils.getPreferredCommentSorting(commentsFragment.getContext()).equals("Default")) {
                commentsFragment.adapter.notifyItemRangeChanged(1, commentsFragment.comments.size());
            }

            //and then perhaps apply an updated sorting
            CommentSorter.sort(commentsFragment.getContext(), commentsFragment.comments);

            boolean storyChanged = JSONParser.updateStoryInformation(commentsFragment.story, jsonObject, forceHeaderRefresh, oldCommentCount, commentsFragment.comments.size());
            if (storyChanged || forceHeaderRefresh) {
                commentsFragment.adapter.notifyItemChanged(0);
            }

            commentsFragment.integratedWebview = commentsFragment.prefIntegratedWebview && commentsFragment.story.isLink;

            if (commentsFragment.integratedWebview && !commentsFragment.initializedWebView) {
                //it's the first time, so we need to re-initialize the recyclerview too
                com.simon.harmonichackernews.view.ViewUtils.initializeWebView(commentsFragment);
                com.simon.harmonichackernews.view.ViewUtils.initializeRecyclerView(commentsFragment);
            }

            if (SettingsUtils.shouldCollapseTopLevel(commentsFragment.getContext())) {
                for (Comment c : commentsFragment.comments) {
                    if (c.depth == 0) {
                        c.expanded = false;
                    }
                }
            }

            commentsFragment.adapter.loadingFailed = false;
            commentsFragment.adapter.loadingFailedServerError = false;

            //Seems like loading went well, lets cache the result
            if (cache) {
                Utils.cacheStory(commentsFragment.getContext(), id, response);
            } else if (restoreScroll) {
                //if we're not caching the result, this means we just loaded an old cache.
                //let's see if we can recover the scroll position.
                if (MainActivity.commentsScrollProgresses != null && !MainActivity.commentsScrollProgresses.isEmpty()) {
                    //we check all of the caches to see if one has the same story ID
                    for (CommentsScrollProgress scrollProgress : MainActivity.commentsScrollProgresses) {
                        if (scrollProgress.storyId == commentsFragment.story.id) {
                            //jackpot! Let's restore the state
                            com.simon.harmonichackernews.view.ViewUtils.restoreScrollProgress(commentsFragment, scrollProgress);
                        }
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            //Show some error, remove things?
            commentsFragment.adapter.loadingFailed = true;
            commentsFragment.adapter.loadingFailedServerError = false;
            commentsFragment.adapter.notifyItemChanged(0);
            commentsFragment.swipeRefreshLayout.setRefreshing(false);
        }

        commentsFragment.adapter.commentsLoaded = true;
        com.simon.harmonichackernews.view.ViewUtils.updateNavigationVisibility(commentsFragment.comments, commentsFragment.scrollNavigation, commentsFragment.showNavButtons);
    }

    public static void loadPollOptions(CommentsFragment commentsFragment) {
        commentsFragment.story.pollOptionArrayList = new ArrayList<>();
        for (int optionId : commentsFragment.story.pollOptions) {
            PollOption pollOption = new PollOption();
            pollOption.loaded = false;
            pollOption.id = optionId;
            commentsFragment.story.pollOptionArrayList.add(pollOption);
        }

        for (int optionId : commentsFragment.story.pollOptions) {
            String url = "https://hacker-news.firebaseio.com/v0/item/" + optionId + ".json";

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
                try {
                    for (int i = 0; i < commentsFragment.story.pollOptionArrayList.size(); i++) {
                        PollOption pollOption = commentsFragment.story.pollOptionArrayList.get(i);

                        if (pollOption.id == optionId) {
                            pollOption.loaded = true;

                            JSONObject jsonObject = new JSONObject(response);
                            pollOption.points = jsonObject.getInt("score");
                            pollOption.text = JSONParser.preprocessHtml(jsonObject.getString("text"));

                            commentsFragment.adapter.notifyItemChanged(0);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {

            });

            stringRequest.setTag(commentsFragment.requestTag);
            commentsFragment.queue.add(stringRequest);
        }
    }

    public static void loadStoryAndComments(final CommentsFragment commentsFragment, final int id, final String oldCachedResponse) {
        String url = "https://hn.algolia.com/api/v1/items/" + id;
        commentsFragment.lastLoaded = System.currentTimeMillis();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (TextUtils.isEmpty(oldCachedResponse) || !oldCachedResponse.equals(response)) {
                        handleJsonResponse(commentsFragment, id, response, true, oldCachedResponse == null, false);
                    }
                    commentsFragment.swipeRefreshLayout.setRefreshing(false);
                }, error -> {
            error.printStackTrace();

            if (error instanceof com.android.volley.TimeoutError) {
                commentsFragment.adapter.loadingFailedServerError = true;
            }

            if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                commentsFragment.adapter.loadingFailedServerError = true;
            }

            commentsFragment.adapter.loadingFailed = true;
            commentsFragment.adapter.notifyItemChanged(0);
            commentsFragment.swipeRefreshLayout.setRefreshing(false);
        });

        if (commentsFragment.story.pollOptions != null) {
            loadPollOptions(commentsFragment);
        }

        if (ArxivAbstractGetter.isValidArxivUrl(commentsFragment.story.url) && SettingsUtils.shouldUseLinkPreviewArxiv(commentsFragment.getContext())) {
            ArxivAbstractGetter.getAbstract(commentsFragment.story.url, commentsFragment.getContext(), new ArxivAbstractGetter.GetterCallback() {
                @Override
                public void onSuccess(ArxivInfo arxivInfo) {
                    commentsFragment.story.arxivInfo = arxivInfo;
                    if (commentsFragment.adapter != null) {
                        commentsFragment.adapter.notifyItemChanged(0);
                    }
                }

                @Override
                public void onFailure(String reason) {
                    //no-op
                }
            });
        } else if (GitHubInfoGetter.isValidGitHubUrl(commentsFragment.story.url) && SettingsUtils.shouldUseLinkPreviewGithub(commentsFragment.getContext())) {
            GitHubInfoGetter.getInfo(commentsFragment.story.url, commentsFragment.getContext(), new GitHubInfoGetter.GetterCallback() {
                @Override
                public void onSuccess(RepoInfo repoInfo) {
                    commentsFragment.story.repoInfo = repoInfo;
                    if (commentsFragment.adapter != null) {
                        commentsFragment.adapter.notifyItemChanged(0);
                    }
                }

                @Override
                public void onFailure(String reason) {
                    //no op
                }
            });
        } else if (WikipediaGetter.isValidWikipediaUrl(commentsFragment.story.url) && SettingsUtils.shouldUseLinkPreviewWikipedia(commentsFragment.getContext())) {
            WikipediaGetter.getInfo(commentsFragment.story.url, commentsFragment.getContext(), new WikipediaGetter.GetterCallback() {
                @Override
                public void onSuccess(WikipediaInfo wikipediaInfo) {
                    commentsFragment.story.wikiInfo = wikipediaInfo;
                    if (commentsFragment.adapter != null) {
                        commentsFragment.adapter.notifyItemChanged(0);
                    }
                }

                @Override
                public void onFailure(String reason) {
                    //no op
                }
            });
        }

        stringRequest.setTag(commentsFragment.requestTag);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        commentsFragment.queue.add(stringRequest);
    }
}
