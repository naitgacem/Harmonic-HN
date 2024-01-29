package com.simon.harmonichackernews;

import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.PathInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.simon.harmonichackernews.adapters.StoryRecyclerViewAdapter;
import com.simon.harmonichackernews.data.Bookmark;
import com.simon.harmonichackernews.data.Story;
import com.simon.harmonichackernews.databinding.FragmentStoriesBinding;
import com.simon.harmonichackernews.network.JSONParser;
import com.simon.harmonichackernews.network.NetworkComponent;
import com.simon.harmonichackernews.utils.AccountUtils;
import com.simon.harmonichackernews.utils.FontUtils;
import com.simon.harmonichackernews.utils.SettingsUtils;
import com.simon.harmonichackernews.utils.StoryUpdate;
import com.simon.harmonichackernews.utils.Utils;
import com.simon.harmonichackernews.utils.ViewUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class StoriesFragment extends Fragment {
    public final static String[] hnUrls = new String[]{Utils.URL_TOP, Utils.URL_NEW, Utils.URL_BEST, Utils.URL_ASK, Utils.URL_SHOW, Utils.URL_JOBS};
    private final static long CLICK_INTERVAL = 350;
    private final String TAG = "StoriesFragment:";
    private final Object requestTag = new Object();
    FragmentStoriesBinding binding;
    long lastLoaded = 0;
    long lastClick = 0;
    private StoryClickListener storyClickListener;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout updateContainer;
    private RecyclerView recyclerView;
    private StoryRecyclerViewAdapter adapter;
    private List<Story> stories;
    private RequestQueue queue;
    private LinearLayoutManager linearLayoutManager;
    private Set<Integer> clickedIds;
    private ArrayList<String> filterWords;
    private ArrayList<String> filterDomains;
    private int minimumScore;
    private boolean hideJobs, alwaysOpenComments, hideClicked;
    private int loadedTo = 0;
    public OnBackPressedCallback backPressedCallback;

    public StoriesFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        clickedIds = SettingsUtils.readIntSetFromSharedPreferences(requireContext(), Utils.KEY_SHARED_PREFERENCES_CLICKED_IDS);
        binding = FragmentStoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        stories = new ArrayList<>();
        setupAdapter();
        setupHeader();
        handleBackPress();
        recyclerView = binding.storiesRecyclerview;
        swipeRefreshLayout = binding.storiesSwipeRefresh;
        updateContainer = binding.storiesUpdateContainer;
        linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        recyclerView.setAdapter(adapter);


        binding.storiesHeaderSearchButton.setOnClickListener(this::onClickSearch);


        swipeRefreshLayout.setOnRefreshListener(this::attemptRefresh);
        ViewUtils.setUpSwipeRefreshWithStatusBarOffset(swipeRefreshLayout);
        ViewUtils.requestApplyInsetsWhenAttached(view);

        binding.storiesUpdateButton.setOnClickListener((v) -> {
            attemptRefresh();
            recyclerView.smoothScrollToPosition(0);
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int lastVisibleItem;

            @Override
            public void onScrolled(@NotNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!adapter.searching) {
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

                    int visibleThreshold = 17;
                    for (int i = loadedTo; i < Math.min(lastVisibleItem + visibleThreshold, stories.size()); i++) {
                        loadedTo = i;
                        loadStory(stories.get(i), 0);
                    }
                }
            }
        });
        queue = NetworkComponent.getRequestQueueInstance(requireContext());

        attemptRefresh();

        StoryUpdate.setStoryUpdatedListener(new StoryUpdate.StoryUpdateListener() {
            @Override
            public void callback(Story story) {
                for (int i = 1; i < stories.size(); i++) {
                    if (story.id == stories.get(i).id) {
                        Story oldStory = stories.get(i);

                        if (!oldStory.title.equals(story.title) || oldStory.descendants != story.descendants || oldStory.score != story.score || oldStory.time != story.time || !oldStory.url.equals(story.url)) {
                            oldStory.title = story.title;
                            oldStory.descendants = story.descendants;
                            oldStory.score = story.score;
                            oldStory.time = story.time;
                            oldStory.url = story.url;

                            adapter.notifyItemChanged(i);
                        }
                        break;
                    }
                }
            }
        });
        if (getActivity() instanceof MainActivity) {
            storyClickListener = (MainActivity) getActivity();
        }
    }

    private void handleBackPress() {
        backPressedCallback = new OnBackPressedCallback(false){
            @Override
            public void handleOnBackPressed() {
                attemptRefresh();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(backPressedCallback);
    }

    private void onClickSearch(View v) {
        getParentFragmentManager().setFragmentResultListener("search_query", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
                String result = bundle.getString("query_term");
                search(result);
            }
        });
        getParentFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.main_fragment_stories_container, SearchFragment.class, null)
                        .addToBackStack("search")
                        .commit();

    }

    private void setupHeader() {
        var ctx = requireContext();
        //--------------------------------------------------------------------------------------------//
        // SETUP THE HEADER HEIGHT
        if(!SettingsUtils.shouldUseCompactHeader(getContext())){
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = ctx.getTheme();
            theme.resolveAttribute(com.google.android.material.R.attr.collapsingToolbarLayoutMediumSize,typedValue, true);

            var params = binding.topBar.getLayoutParams();
            params.height = TypedValue.complexToDimensionPixelSize(typedValue.data, getResources().getDisplayMetrics());
        }
        //--------------------------------------------------------------------------------------------//
        // SETUP THE SPINNER
        var sortingOptions = ctx.getResources().getStringArray(R.array.sorting_options);
        var typeAdapterList = new ArrayList<CharSequence>(Arrays.asList(sortingOptions));
        var typeAdapter = new ArrayAdapter<>(ctx, R.layout.spinner_top_layout, R.id.selection_dropdown_item_textview, typeAdapterList);
        typeAdapter.setDropDownViewResource(R.layout.spinner_item_layout);
        Spinner typeSpinner = binding.storiesHeaderSpinner;
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i != adapter.type) {
                    adapter.type = i;
                    attemptRefresh();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        binding.storiesHeaderSpinner.setSelection(getPreferredTypeIndex());
        //---------------------------------------------------------------------------------------------//
        // SETUP MORE BUTTON
        binding.storiesHeaderMore.setOnClickListener(this::moreClick);
        //--------------------------------------------------------------------------------------------//
        //SETUP TOOLTIPS
        TooltipCompat.setTooltipText(binding.storiesHeaderMore, "More");
        TooltipCompat.setTooltipText(binding.storiesHeaderSearchButton, "Search stories");

    }

    private int getPreferredTypeIndex() {
        String[] sortingOptions = getResources().getStringArray(R.array.sorting_options);
        ArrayList<CharSequence> typeAdapterList = new ArrayList<>(Arrays.asList(sortingOptions));
        return typeAdapterList.indexOf(SettingsUtils.getPreferredStoryType(getContext()));
    }

    private void setupAdapter() {
        adapter = new StoryRecyclerViewAdapter(
                stories,
                SettingsUtils.shouldShowPoints(getContext()),
                SettingsUtils.shouldShowCommentsCount(getContext()),
                SettingsUtils.shouldUseCompactView(getContext()),
                SettingsUtils.shouldShowThumbnails(getContext()),
                SettingsUtils.shouldShowIndex(getContext()),
                SettingsUtils.shouldUseLeftAlign(getContext()),
                SettingsUtils.getPreferredHotness(getContext()),
                SettingsUtils.getPreferredFaviconProvider(getContext()),
                null,
                getPreferredTypeIndex()
        );

        adapter.setOnLinkClickListener(position -> {
            if (position == RecyclerView.NO_POSITION) {
                return;
            }

            if (alwaysOpenComments) {
                clickedComments(position);
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastClick > CLICK_INTERVAL) {
                lastClick = now;
            } else {
                return;
            }

            Story story = stories.get(position);
            if (story.loaded) {
                story.clicked = true;
                clickedIds.add(story.id);

                if (story.isLink) {
                    if (SettingsUtils.shouldUseIntegratedWebView(getContext())) {
                        openComments(story, position, true);
                    } else {
                        Utils.launchCustomTab(getContext(), story.url);
                    }
                } else {
                    openComments(story, position, false);
                }

                adapter.notifyItemChanged(position);
            } else if (story.loadingFailed) {
                story.loadingFailed = false;
                loadStory(story, 0);
                adapter.notifyItemChanged(position);
            }
        });
        adapter.setOnCommentClickListener(this::clickedComments);
        adapter.setOnRefreshListener(this::attemptRefresh);
        adapter.setOnLongClickListener(new StoryRecyclerViewAdapter.LongClickCoordinateListener() {
            @Override
            public boolean onLongClick(View v, int position, int x, int y) {
                if (position == RecyclerView.NO_POSITION) {
                    return false;
                }

                Context context = v.getContext();

                PopupMenu popupMenu = new PopupMenu(context, v);

                Story story = stories.get(position);
                boolean oldClicked = story.clicked;

                popupMenu.getMenu().add(oldClicked ? "Mark as unread" : "Mark as read").setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        story.clicked = !oldClicked;
                        if (oldClicked) {
                            clickedIds.remove(story.id);
                        } else {
                            clickedIds.add(story.id);
                        }

                        adapter.notifyItemChanged(position);
                        return true;
                    }
                });
                popupMenu.show();
                return false;
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        //----------------------------------------------------------------------------------------//
        // In this section we are comparing the current settings with the current state
        // Updating as necessary
        //----------------------------------------------------------------------------------------//
        filterWords = Utils.getFilterWords(getContext());
        minimumScore = Utils.getMinScore(getContext());
        filterDomains = Utils.getFilterDomains(getContext());
        hideJobs = SettingsUtils.shouldHideJobs(getContext());
        hideClicked = SettingsUtils.shouldHideClicked(getContext());
        alwaysOpenComments = SettingsUtils.shouldAlwaysOpenComments(getContext());

        long timeDiff = System.currentTimeMillis() - lastLoaded;

        // if more than 1 hr
        if (timeDiff > 1000 * 60 * 60 && !adapter.searching && adapter.type != SettingsUtils.getBookmarksIndex(getResources()) && !currentTypeIsAlgolia()) {
            showUpdateButton();
        }
        binding.getRoot().invalidate();
        if (adapter.showPoints != SettingsUtils.shouldShowPoints(getContext())) {
            adapter.showPoints = !adapter.showPoints;
            adapter.notifyItemRangeChanged(0, stories.size());
        }

        if (adapter.showCommentsCount != SettingsUtils.shouldShowCommentsCount(getContext())) {
            adapter.showCommentsCount = !adapter.showCommentsCount;
            adapter.notifyItemRangeChanged(0, stories.size());
        }

        if (adapter.compactView != SettingsUtils.shouldUseCompactView(getContext())) {
            adapter.compactView = !adapter.compactView;
            adapter.notifyItemRangeChanged(0, stories.size());
        }

        if (adapter.thumbnails != SettingsUtils.shouldShowThumbnails(getContext())) {
            adapter.thumbnails = !adapter.thumbnails;
            adapter.notifyItemRangeChanged(0, stories.size());
        }

        if (adapter.showIndex != SettingsUtils.shouldShowIndex(getContext())) {
            adapter.showIndex = !adapter.showIndex;
            adapter.notifyItemRangeChanged(0, stories.size());
        }

        if (adapter.leftAlign != SettingsUtils.shouldUseLeftAlign(getContext())) {
            adapter.leftAlign = !adapter.leftAlign;
            setupAdapter();
            recyclerView.setAdapter(adapter);
        }

        if (TextUtils.isEmpty(FontUtils.font) || !FontUtils.font.equals(SettingsUtils.getPreferredFont(getContext()))) {
            FontUtils.init(getContext());
            adapter.notifyItemRangeChanged(0, stories.size());
        }

        if (adapter.hotness != SettingsUtils.getPreferredHotness(getContext())) {
            adapter.hotness = SettingsUtils.getPreferredHotness(getContext());
            adapter.notifyItemRangeChanged(0, stories.size());
        }

        if (hideJobs != SettingsUtils.shouldHideJobs(getContext())) {
            hideJobs = !hideJobs;
            attemptRefresh();
        }

        if (!Objects.equals(adapter.faviconProvider, SettingsUtils.getPreferredFaviconProvider(getContext()))) {
            adapter.faviconProvider = SettingsUtils.getPreferredFaviconProvider(getContext());
            adapter.notifyItemRangeChanged(0, stories.size());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SettingsUtils.saveIntSetToSharedPreferences(getContext(), Utils.KEY_SHARED_PREFERENCES_CLICKED_IDS, clickedIds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (queue != null) {
            queue.cancelAll(requestTag);
        }
        binding = null;
    }

    private void clickedComments(int position) {
        //prevent double clicks
        long now = System.currentTimeMillis();
        if (now - lastClick > CLICK_INTERVAL) {
            lastClick = now;
        } else {
            return;
        }

        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        Story story = stories.get(position);
        if (story.loaded) {
            story.clicked = true;
            clickedIds.add(story.id);

            openComments(story, position, false);

            adapter.notifyItemChanged(position);
        }
    }

    private void loadStory(Story story, final int attempt) {
        if (story.loaded || attempt >= 3) {
            return;
        }

        String url = "https://hacker-news.firebaseio.com/v0/item/" + story.id + ".json";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            try {
                int index = stories.indexOf(story);

                if (!JSONParser.updateStoryWithHNJson(response, story)) {
                    stories.remove(story);
                    adapter.notifyItemRemoved(index);
                    loadedTo = Math.max(0, loadedTo - 1);
                    return;
                }

                //lets check if we should remove the post because of filter
                for (String phrase : filterWords) {
                    if (story.title.toLowerCase().contains(phrase.toLowerCase())) {
                        stories.remove(story);
                        adapter.notifyItemRemoved(index);
                        loadedTo = Math.max(0, loadedTo - 1);
                        return;
                    }
                }
                // or domain name
                for (String phrase : filterDomains) {
                    if (story.url.toLowerCase().contains(phrase.toLowerCase())) {
                        stories.remove(story);
                        adapter.notifyItemRemoved(index);
                        loadedTo = Math.max(0, loadedTo - 1);
                        return;
                    }
                }

                //or because it's a job
                if (hideJobs && adapter.type != SettingsUtils.getJobsIndex(getResources()) && (story.isJob || story.by.equals("whoishiring"))) {
                    stories.remove(story);
                    adapter.notifyItemRemoved(index);
                    loadedTo = Math.max(0, loadedTo - 1);
                    return;
                }

                //or because it's less than the minimum score
                if (story.score < minimumScore) {
                    stories.remove(story);
                    adapter.notifyItemRemoved(index);
                    loadedTo = Math.max(0, loadedTo - 1);
                    return;
                }

                adapter.notifyItemChanged(index);
            } catch (JSONException e) {
                e.printStackTrace();
                Utils.log("Failed to load story with id: " + story.id);
                adapter.notifyDataSetChanged();
            }
        }, error -> {
            error.printStackTrace();
            story.loadingFailed = true;
            adapter.notifyItemChanged(stories.indexOf(story));
            loadStory(story, attempt + 1);
        });

        stringRequest.setTag(requestTag);
        queue.add(stringRequest);
    }

    public void moreClick(View view) {
        PopupMenu popup = new PopupMenu(requireActivity(), view);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_settings) {
                    requireActivity().startActivity(new Intent(requireActivity(), SettingsActivity.class));
                } else if (item.getItemId() == R.id.menu_log) {
                    if (TextUtils.isEmpty(AccountUtils.getAccountUsername(requireActivity()))) {
                        AccountUtils.showLoginPrompt(requireActivity().getSupportFragmentManager());
                    } else {
                        AccountUtils.deleteAccountDetails(requireActivity());
                        Toast.makeText(getContext(), "Logged out", Toast.LENGTH_SHORT).show();
                    }
                } else if (item.getItemId() == R.id.menu_profile) {
                    UserDialogFragment.showUserDialog(requireActivity().getSupportFragmentManager(), AccountUtils.getAccountUsername(requireActivity()));
                } else if (item.getItemId() == R.id.menu_submit) {
                    Intent submitIntent = new Intent(getContext(), ComposeActivity.class);
                    submitIntent.putExtra(ComposeActivity.EXTRA_TYPE, ComposeActivity.TYPE_POST);
                    startActivity(submitIntent);
                }
                return true;
            }
        });
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());

        Menu menu = popup.getMenu();

        boolean loggedIn = !TextUtils.isEmpty(AccountUtils.getAccountUsername(requireActivity()));

        menu.findItem(R.id.menu_log).setTitle(loggedIn ? "Log out" : "Log in");
        menu.findItem(R.id.menu_profile).setVisible(loggedIn);
        menu.findItem(R.id.menu_submit).setVisible(loggedIn);

        popup.show();
    }

    public void attemptRefresh() {
        backPressedCallback.setEnabled(false);
        hideUpdateButton();
        binding.loadingSpinner.hide();
        binding.storiesHeaderSpinner.setVisibility(View.VISIBLE);
        binding.searchTitle.setVisibility(View.GONE);

        swipeRefreshLayout.setRefreshing(true);

        //cancel all ongoing
        queue.cancelAll(requestTag);

        if (currentTypeIsAlgolia()) {
            //algoliaStuff
            int currentTime = (int) (System.currentTimeMillis() / 1000);
            int startTime = currentTime;
            if (adapter.type == 1) {
                startTime = currentTime - 60 * 60 * 24;
            } else if (adapter.type == 2) {
                startTime = currentTime - 60 * 60 * 48;
            } else if (adapter.type == 3) {
                startTime = currentTime - 60 * 60 * 24 * 7;
            }

            loadTopStoriesSince(startTime);

            return;
        }

        lastLoaded = System.currentTimeMillis();

        if (adapter.type == SettingsUtils.getBookmarksIndex(getResources())) {
            //lets load bookmarks instead - or rather add empty stories with correct id:s and start loading them
            adapter.notifyItemRangeRemoved(0, stories.size());
            loadedTo = 0;

            stories.clear();

            ArrayList<Bookmark> bookmarks = Utils.loadBookmarks(getContext(), true);

            for (int i = 0; i < bookmarks.size(); i++) {
                Story s = new Story("Loading...", bookmarks.get(i).id, false, false);

                stories.add(s);
                adapter.notifyItemInserted(i);
                if (i < 20) {
                    loadStory(stories.get(i), 0);
                }
            }

            adapter.notifyItemChanged(0);
            swipeRefreshLayout.setRefreshing(false);

            return;
        }

        // if none of the above, do a normal loading
        StringRequest stringRequest = new StringRequest(Request.Method.GET, hnUrls[adapter.type == 0 ? 0 : adapter.type - 3], response -> {
            swipeRefreshLayout.setRefreshing(false);
            try {
                JSONArray jsonArray = new JSONArray(response);

                loadedTo = 0;

                adapter.notifyItemRangeRemoved(0, stories.size());

                stories.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    int id = Integer.parseInt(jsonArray.get(i).toString());
                    if (hideClicked && clickedIds.contains(id)) {
                        continue;
                    }

                    Story s = new Story("Loading...", id, false, clickedIds.contains(id));
                    //let's try to fill this with old information if possible

                    String cachedResponse = Utils.loadCachedStory(getContext(), id);
                    if (cachedResponse != null && !cachedResponse.equals(JSONParser.ALGOLIA_ERROR_STRING)) {
                        JSONParser.updateStoryWithAlgoliaResponse(s, cachedResponse);
                    }

                    stories.add(s);
                    adapter.notifyItemInserted(i);
                }

                if (adapter.loadingFailed) {
                    adapter.loadingFailed = false;
                    adapter.loadingFailedServerError = false;
                }

                adapter.notifyItemChanged(0);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            swipeRefreshLayout.setRefreshing(false);
            adapter.loadingFailed = true;
            adapter.notifyItemChanged(0);
        });

        adapter.notifyItemChanged(0);
        stringRequest.setTag(requestTag);
        queue.add(stringRequest);
    }

    private void loadTopStoriesSince(int start_i) {
        loadAlgolia("https://hn.algolia.com/api/v1/search?tags=story&numericFilters=created_at_i>" + start_i + "&hitsPerPage=200");
    }

    private void search(String query) {
        getParentFragmentManager().popBackStack();
        if (query == null || query.isEmpty()) {
            return;
        }
        binding.storiesHeaderSpinner.setVisibility(View.GONE);
        binding.searchTitle.setVisibility(View.VISIBLE);
        binding.loadingSpinner.show();
        String displayQuery = String.format(requireContext().getString(R.string.search_query_display), query);
        binding.searchTitleQuery.setText(displayQuery);
        backPressedCallback.setEnabled(true);
        loadAlgolia("https://hn.algolia.com/api/v1/search_by_date?query=" + query + "&tags=story&hitsPerPage=200");
    }

    private void loadAlgolia(String url) {
        swipeRefreshLayout.setEnabled(true);
        swipeRefreshLayout.setRefreshing(true);
        stories.clear();
        adapter.notifyDataSetChanged(); //necessary to avoid crash with RV
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, response -> {
            swipeRefreshLayout.setRefreshing(false);
            try {
                int oldSize = stories.size();
                adapter.notifyItemRangeRemoved(0, oldSize);

                stories.addAll(JSONParser.algoliaJsonToStories(response));
                binding.loadingSpinner.hide();
                Iterator<Story> iterator = stories.iterator();
                while (iterator.hasNext()) {
                    Story story = iterator.next();
                    story.clicked = clickedIds.contains(story.id);

                    if (hideClicked && story.clicked) {
                        iterator.remove();
                    }
                }

                adapter.loadingFailed = false;
                adapter.loadingFailedServerError = false;

                adapter.notifyItemRangeInserted(0, stories.size());
                adapter.notifyItemChanged(0);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }, error -> {
            if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                adapter.loadingFailedServerError = true;
            }

            error.printStackTrace();
            swipeRefreshLayout.setRefreshing(false);
            adapter.loadingFailed = true;
            adapter.notifyItemChanged(0);
        });

        adapter.notifyItemChanged(0);

        stringRequest.setTag(requestTag);
        queue.add(stringRequest);
    }

    public boolean currentTypeIsAlgolia() {
        return 0 < adapter.type && 4 > adapter.type;
    }


    private void hideUpdateButton() {
        if (updateContainer.getVisibility() == View.VISIBLE) {

            float endYPosition = getResources().getDisplayMetrics().heightPixels - updateContainer.getY() + updateContainer.getHeight() + ViewUtils.getNavigationBarHeight(getResources());
            PathInterpolator pathInterpolator = new PathInterpolator(0.3f, 0f, 0.8f, 0.15f);

            ObjectAnimator yAnimator = ObjectAnimator.ofFloat(updateContainer, "translationY", endYPosition);
            yAnimator.setDuration(200);

            yAnimator.setInterpolator(pathInterpolator);

            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(updateContainer, "alpha", 1.0f, 0.0f);
            alphaAnimator.setDuration(300);
            alphaAnimator.setInterpolator(pathInterpolator);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(yAnimator, alphaAnimator);

            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    updateContainer.setVisibility(View.GONE);
                    updateContainer.setTranslationY(0);
                    updateContainer.setAlpha(1f);
                }
            });

            animatorSet.start();
        }
    }

    private void showUpdateButton() {
        if (updateContainer.getVisibility() != View.VISIBLE) {
            updateContainer.setVisibility(View.VISIBLE);

            AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(300);
            anim.setRepeatMode(Animation.REVERSE);
            updateContainer.startAnimation(anim);
        }
    }

    private void openComments(Story story, int pos, boolean showWebsite) {
        storyClickListener.openStory(story, pos, showWebsite);
    }

    public interface StoryClickListener {
        void openStory(Story story, int pos, boolean showWebsite);
    }
}
