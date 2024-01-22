package com.simon.harmonichackernews.utils;

import androidx.core.util.Pair;

import com.simon.harmonichackernews.R;
import com.simon.harmonichackernews.data.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentsUtils {
    public static List<Pair<String, Integer>> getPossibleActions(Comment comment) {
        List<Pair<String, Integer>> itemsList = new ArrayList<>();

        itemsList.add(new Pair<>("View user (" + comment.by + ")", R.drawable.ic_action_user));
        itemsList.add(new Pair<>("Share comment link", R.drawable.ic_action_share));
        itemsList.add(new Pair<>("Copy text", R.drawable.ic_action_copy));
        itemsList.add(new Pair<>("Select text", R.drawable.ic_action_select));
        itemsList.add(new Pair<>("Vote up", R.drawable.ic_action_thumbs_up));
        itemsList.add(new Pair<>("Unvote", R.drawable.ic_action_thumbs));
        itemsList.add(new Pair<>("Vote down", R.drawable.ic_action_thumb_down));
        itemsList.add(new Pair<>("Bookmark", R.drawable.ic_action_bookmark_border));
        if(comment.parentComment != null){
            itemsList.add(new Pair<>("Parent", R.drawable.ic_action_arrow_up));
        }
        if(comment.rootComment != null){
            itemsList.add(new Pair<>("Root", R.drawable.ic_action_arrow_up));
        }
        if(Utils.timeInSecondsMoreThanTwoWeeksAgo(comment.time)){
            new Pair<>("Reply", R.drawable.ic_action_reply);
        }
        return itemsList;
    }
}
