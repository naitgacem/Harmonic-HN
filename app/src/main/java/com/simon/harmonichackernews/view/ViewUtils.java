package com.simon.harmonichackernews.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.simon.harmonichackernews.ComposeActivity;
import com.simon.harmonichackernews.R;
import com.simon.harmonichackernews.UserDialogFragment;
import com.simon.harmonichackernews.data.Comment;
import com.simon.harmonichackernews.network.UserActions;
import com.simon.harmonichackernews.utils.AccountUtils;
import com.simon.harmonichackernews.utils.CommentAction;
import com.simon.harmonichackernews.utils.CommentsUtils;
import com.simon.harmonichackernews.utils.DialogUtils;
import com.simon.harmonichackernews.utils.ShareUtils;
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
                    case BOOKMARK -> Utils.addBookmark(ctx, comment.id);
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




}
