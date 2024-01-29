package com.simon.harmonichackernews;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.simon.harmonichackernews.databinding.FragmentSearchBinding;

import java.util.ArrayList;

public class SearchFragment extends Fragment {
    FragmentSearchBinding binding;
    String searchQuery;
    ArrayList<String> tags;
    private final String SHOW_HN = "show_hn";
    private final String ASK_HN = "ask_hn";
    private final String FRONT_PAGE = "front_page";
    private final String STORY = "story";
    private final String COMMENT = "comment";
    private final String POLL = "poll";

    public SearchFragment() {
        super();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.getRoot().setClickable(true);
        searchQuery = "";
        tags = new ArrayList<>();
        EditText searchBar  = binding.searchEditText;
        searchBar.requestFocus();
        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                searchQuery = searchBar.getText().toString();
                return sendResult();
            }
        });
        binding.searchNormalToolbar.setNavigationOnClickListener(this::dismissSearch);
        binding.postSpecialTagToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                int index = group.indexOfChild(group.findViewById(checkedId));
                switch (index){
                    case 0 -> {
                        updateTag(isChecked, SHOW_HN);
                    }
                    case 1 -> {
                        updateTag(isChecked, ASK_HN);
                    }
                    case 2 -> {
                        updateTag(isChecked, FRONT_PAGE);
                    }
                }
            }
        });

        binding.postTypeToggleGroup.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                int index = group.indexOfChild(group.findViewById(checkedId));
                switch (index){
                    case 0 -> {
                        updateTag(isChecked, STORY);
                    }
                    case 1 -> {
                        updateTag(isChecked, COMMENT);
                    }
                    case 2 -> {
                        updateTag(isChecked, POLL);
                    }
                }
            }
        });
    }
    private void updateTag(boolean isChecked, String tag){
        if(isChecked){
            tags.add(tag);
        } else {
            tags.remove(tag);
        }
    }
    private boolean sendResult() {
        Bundle result = new Bundle();
        result.putString("query_term", searchQuery);
        result.putStringArrayList("tags", tags);
        getParentFragmentManager().setFragmentResult("search_query", result);
        return true;
    }
    private void dismissSearch(View v){
        searchQuery = "";
        sendResult();
    }
}
