/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.htmltextview;

import android.content.Context;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.simon.harmonichackernews.linkpreview.NitterGetter;
import com.simon.harmonichackernews.utils.SettingsUtils;

public class HtmlFormatter {

    private HtmlFormatter() {
    }

    interface TagClickListenerProvider {
        OnClickATagListener provideTagClickListener();
    }

    public static Spanned formatHtml(Context context, @Nullable String html, ImageGetter imageGetter, ClickableTableSpan clickableTableSpan, DrawTableLinkSpan drawTableLinkSpan, TagClickListenerProvider tagClickListenerProvider, float indent, boolean removeTrailingWhiteSpace) {
        final HtmlTagHandler htmlTagHandler = new HtmlTagHandler();
        htmlTagHandler.setClickableTableSpan(clickableTableSpan);
        htmlTagHandler.setDrawTableLinkSpan(drawTableLinkSpan);
        htmlTagHandler.setOnClickATagListenerProvider(tagClickListenerProvider);
        htmlTagHandler.setListIndentPx(indent);
        htmlTagHandler.shouldRedirectTwitter = SettingsUtils.shouldRedirectNitter(context.getApplicationContext());
        html = htmlTagHandler.overrideTags(html);

        Spanned formattedHtml;
        if (removeTrailingWhiteSpace) {
            formattedHtml = removeHtmlBottomPadding(Html.fromHtml(html, imageGetter, new WrapperContentHandler(htmlTagHandler)));
        } else {
            formattedHtml = Html.fromHtml(html, imageGetter, new WrapperContentHandler(htmlTagHandler));
        }

        // Replace non breaking spaces with breaking spaces.
        // They make unexpected line-breaks even in the middle of a word.
        SpannableStringBuilder builder = new SpannableStringBuilder(formattedHtml);
        replaceNonBreakingSpaces(builder);
        return (Spanned) builder.subSequence(0, builder.length());
    }

    /**
     * Html.fromHtml sometimes adds extra space at the bottom.
     * This methods removes this space again.
     * See https://github.com/SufficientlySecure/html-textview/issues/19
     */
    @Nullable
    private static Spanned removeHtmlBottomPadding(@Nullable Spanned text) {
        if (text == null) {
            return null;
        }

        while (text.length() > 0 && text.charAt(text.length() - 1) == '\n') {
            text = (Spanned) text.subSequence(0, text.length() - 1);
        }
        return text;
    }

    private static void replaceNonBreakingSpaces(SpannableStringBuilder builder) {
        int start = 0;
        while (start < builder.length()) {
            int index = builder.toString().indexOf('\u00A0', start);
            if (index != -1) {
                builder.replace(index, index + 1, " ");
                start = index + 1;
            } else {
                break;
            }
        }
    }
}
