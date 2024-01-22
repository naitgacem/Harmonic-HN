package com.simon.harmonichackernews.utils;

import android.os.Bundle;

import androidx.core.util.Pair;

import com.simon.harmonichackernews.R;
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
}
