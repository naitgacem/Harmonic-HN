package com.simon.harmonichackernews;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.simon.harmonichackernews.databinding.FragmentSearchBinding;
import com.simon.harmonichackernews.databinding.FragmentStoriesBinding;
import com.simon.harmonichackernews.utils.SettingsUtils;
import com.simon.harmonichackernews.utils.Utils;

public class SearchFragment extends Fragment {
    FragmentSearchBinding binding;
    String searchQuery;
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
    }

    private boolean sendResult() {
        Bundle result = new Bundle();
        result.putString("query_term", searchQuery);
        getParentFragmentManager().setFragmentResult("search_query", result);
        return true;
    }
    private void dismissSearch(View v){
        searchQuery = "";
        sendResult();
    }
}
