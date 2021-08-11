/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RadialProgressView;

public class ChatLoadingCell extends FrameLayout {

    private FrameLayout frameLayout;
    private RadialProgressView progressBar;

    public ChatLoadingCell(Context context, View parent) {
        super(context);

        frameLayout = new FrameLayout(context);
        frameLayout.setBackground(Theme.createServiceDrawable(AndroidUtilities.dp(18), frameLayout, parent));
        addView(frameLayout, LayoutHelper.createFrame(36, 36, Gravity.CENTER));

        progressBar = new RadialProgressView(context);
        progressBar.setSize(AndroidUtilities.dp(28));
        progressBar.setProgressColor(Theme.getColor(Theme.key_chat_serviceText));
        frameLayout.addView(progressBar, LayoutHelper.createFrame(32, 32, Gravity.CENTER));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), MeasureSpec.EXACTLY));
    }

    public void setProgressVisible(boolean value) {
        frameLayout.setVisibility(value ? VISIBLE : INVISIBLE);
    }
}
