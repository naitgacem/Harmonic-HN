package com.simon.harmonichackernews.data;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.simon.harmonichackernews.utils.CommentsUtils;
import com.simon.harmonichackernews.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

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

    public Story() {
    }

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

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Story story)) return false;

        return descendants == story.descendants && id == story.id && score == story.score && time == story.time && loaded == story.loaded && clicked == story.clicked && isLink == story.isLink && isJob == story.isJob && loadingFailed == story.loadingFailed && isComment == story.isComment && parentId == story.parentId && commentMasterId == story.commentMasterId && Objects.equals(by, story.by) && Objects.equals(title, story.title) && Objects.equals(pdfTitle, story.pdfTitle) && Objects.equals(url, story.url) && Arrays.equals(kids, story.kids) && Arrays.equals(pollOptions, story.pollOptions) && Objects.equals(pollOptionArrayList, story.pollOptionArrayList) && Objects.equals(text, story.text) && Objects.equals(repoInfo, story.repoInfo) && Objects.equals(arxivInfo, story.arxivInfo) && Objects.equals(wikiInfo, story.wikiInfo) && Objects.equals(nitterInfo, story.nitterInfo) && Objects.equals(commentMasterTitle, story.commentMasterTitle) && Objects.equals(commentMasterUrl, story.commentMasterUrl);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(by);
        result = 31 * result + descendants;
        result = 31 * result + id;
        result = 31 * result + score;
        result = 31 * result + time;
        result = 31 * result + Objects.hashCode(title);
        result = 31 * result + Objects.hashCode(pdfTitle);
        result = 31 * result + Objects.hashCode(url);
        result = 31 * result + Arrays.hashCode(kids);
        result = 31 * result + Arrays.hashCode(pollOptions);
        result = 31 * result + Objects.hashCode(pollOptionArrayList);
        result = 31 * result + Boolean.hashCode(loaded);
        result = 31 * result + Boolean.hashCode(clicked);
        result = 31 * result + Objects.hashCode(text);
        result = 31 * result + Objects.hashCode(repoInfo);
        result = 31 * result + Objects.hashCode(arxivInfo);
        result = 31 * result + Objects.hashCode(wikiInfo);
        result = 31 * result + Objects.hashCode(nitterInfo);
        result = 31 * result + Boolean.hashCode(isLink);
        result = 31 * result + Boolean.hashCode(isJob);
        result = 31 * result + Boolean.hashCode(loadingFailed);
        result = 31 * result + Boolean.hashCode(isComment);
        result = 31 * result + parentId;
        result = 31 * result + Objects.hashCode(commentMasterTitle);
        result = 31 * result + commentMasterId;
        result = 31 * result + Objects.hashCode(commentMasterUrl);
        return result;
    }
}