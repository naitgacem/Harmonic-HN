package com.simon.harmonichackernews.data;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.simon.harmonichackernews.utils.CommentsUtils;
import com.simon.harmonichackernews.utils.Utils;

import java.util.ArrayList;

public class Story {
    public String by;
    public int descendants;
    public int id;
    public int score;
    public int time;
    public String title;
    public String pdfTitle;
    public String url;
    public int[] kids;
    public int[] pollOptions;
    public ArrayList<PollOption> pollOptionArrayList;
    public boolean loaded;
    public boolean clicked;
    public String text;
    public RepoInfo repoInfo;
    public ArxivInfo arxivInfo;
    public WikipediaInfo wikiInfo;
    public NitterInfo nitterInfo;
    public boolean isLink;
    public boolean isJob = false;
    public boolean loadingFailed = false;
    public boolean isComment = false;
    public int parentId = 0; // 0 for top level stories.
    public String commentMasterTitle;
    public int commentMasterId;
    public String commentMasterUrl;

    public Story() {}

    public Story(String title, int id, boolean loaded, boolean clicked) {
        this.title = title;
        this.id = id;
        this.loaded = loaded;
        this.clicked = clicked;
    }

    public void update(String by, int id, int score, int time, String title) {
        this.by = by;
        this.id = id;
        this.score = score;
        this.time = time;
        this.title = title;
    }

    public String getTimeFormatted() {
        return Utils.getTimeAgo(this.time);
    }

    @NonNull
    @Override
    public String toString() {
        return title;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(CommentsUtils.EXTRA_TITLE, title);
        bundle.putString(CommentsUtils.EXTRA_PDF_TITLE, pdfTitle);
        bundle.putString(CommentsUtils.EXTRA_BY, by);
        bundle.putString(CommentsUtils.EXTRA_URL, url);
        bundle.putInt(CommentsUtils.EXTRA_TIME, time);
        bundle.putIntArray(CommentsUtils.EXTRA_KIDS, kids);
        bundle.putIntArray(CommentsUtils.EXTRA_POLL_OPTIONS, pollOptions);
        bundle.putInt(CommentsUtils.EXTRA_DESCENDANTS, descendants);
        bundle.putInt(CommentsUtils.EXTRA_ID, id);
        bundle.putInt(CommentsUtils.EXTRA_SCORE, score);
        bundle.putString(CommentsUtils.EXTRA_TEXT, text);
        bundle.putBoolean(CommentsUtils.EXTRA_IS_LINK, isLink);
        bundle.putBoolean(CommentsUtils.EXTRA_IS_COMMENT, isComment);
        bundle.putInt(CommentsUtils.EXTRA_PARENT_ID, parentId);
        return bundle;
    }

    public boolean hasExtraInfo() {
        return arxivInfo != null || repoInfo != null || wikiInfo != null || nitterInfo != null;
    }

}