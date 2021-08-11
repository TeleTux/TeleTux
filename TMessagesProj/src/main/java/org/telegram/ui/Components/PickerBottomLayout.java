/*
 * This is the source code of Telegram for Android v. 2.0.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

public class PickerBottomLayout extends FrameLayout {

    public LinearLayout doneButton;
    public TextView cancelButton;
    public LinearLayout middleButton;
    public TextView middleButtonTextView;
    public TextView doneButtonTextView;
    public TextView doneButtonBadgeTextView;

    public PickerBottomLayout(Context context) {
        this(context, true);
    }

    public PickerBottomLayout(Context context, boolean darkTheme) {
        super(context);

        setBackgroundColor(Theme.getColor(darkTheme ? Theme.key_dialogBackground : Theme.key_windowBackgroundWhite));

        cancelButton = new TextView(context);
        cancelButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        cancelButton.setTextColor(Theme.getColor(Theme.key_picker_enabledButton));
        cancelButton.setGravity(Gravity.CENTER);
        cancelButton.setBackgroundDrawable(Theme.createSelectorDrawable(0x0f000000, 0));
        cancelButton.setPadding(AndroidUtilities.dp(33), 0, AndroidUtilities.dp(33), 0);
        cancelButton.setText(LocaleController.getString("Cancel", R.string.Cancel).toUpperCase());
        cancelButton.setTypeface(AndroidUtilities.getTypeface("fonts/Vazir-Regular.ttf"));
        addView(cancelButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

        LinearLayout rightLayout = new LinearLayout(context);
        rightLayout.setOrientation(LinearLayout.HORIZONTAL);
        rightLayout.setGravity(Gravity.CENTER_VERTICAL);
        addView(rightLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.RIGHT));

        middleButton = new LinearLayout(context);
        middleButton.setOrientation(LinearLayout.HORIZONTAL);
        middleButton.setBackgroundDrawable(Theme.createSelectorDrawable(0x0f000000, 0));
        middleButton.setPadding(AndroidUtilities.dp(33), 0, AndroidUtilities.dp(33), 0);
        middleButton.setVisibility(GONE);
        rightLayout.addView(middleButton);

        middleButtonTextView = new TextView(context);
        middleButtonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        middleButtonTextView.setTextColor(Theme.getColor(Theme.key_picker_enabledButton));
        middleButtonTextView.setGravity(Gravity.CENTER);
        middleButtonTextView.setCompoundDrawablePadding(AndroidUtilities.dp(8));
        middleButtonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/Vazir-Regular.ttf"));
        middleButton.addView(middleButtonTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        doneButton = new LinearLayout(context);
        doneButton.setOrientation(LinearLayout.HORIZONTAL);
        doneButton.setBackgroundDrawable(Theme.createSelectorDrawable(0x0f000000, 0));
        doneButton.setPadding(AndroidUtilities.dp(33), 0, AndroidUtilities.dp(33), 0);
        rightLayout.addView(doneButton);

        doneButtonBadgeTextView = new TextView(context);
        doneButtonBadgeTextView.setTypeface(AndroidUtilities.getTypeface("fonts/Vazir-Regular.ttf"));
        doneButtonBadgeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        doneButtonBadgeTextView.setTextColor(Theme.getColor(Theme.key_picker_badgeText));
        doneButtonBadgeTextView.setGravity(Gravity.CENTER);
        Drawable drawable = Theme.createRoundRectDrawable(AndroidUtilities.dp(11), Theme.getColor(Theme.key_picker_badge));
        doneButtonBadgeTextView.setBackgroundDrawable(drawable);
        doneButtonBadgeTextView.setMinWidth(AndroidUtilities.dp(23));
        doneButtonBadgeTextView.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), AndroidUtilities.dp(1));
        doneButton.addView(doneButtonBadgeTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 23, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        doneButtonTextView = new TextView(context);
        doneButtonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        doneButtonTextView.setTextColor(Theme.getColor(Theme.key_picker_enabledButton));
        doneButtonTextView.setGravity(Gravity.CENTER);
        doneButtonTextView.setCompoundDrawablePadding(AndroidUtilities.dp(8));
        doneButtonTextView.setText(LocaleController.getString("Send", R.string.Send).toUpperCase());
        doneButtonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/Vazir-Regular.ttf"));
        doneButton.addView(doneButtonTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
    }

    public void updateSelectedCount(int count, boolean disable) {
        if (count == 0) {
            doneButtonBadgeTextView.setVisibility(View.GONE);

            if (disable) {
                doneButtonTextView.setTag(Theme.key_picker_disabledButton);
                doneButtonTextView.setTextColor(Theme.getColor(Theme.key_picker_disabledButton));
                doneButton.setEnabled(false);
            } else {
                doneButtonTextView.setTag(Theme.key_picker_enabledButton);
                doneButtonTextView.setTextColor(Theme.getColor(Theme.key_picker_enabledButton));
            }
        } else {
            doneButtonBadgeTextView.setVisibility(View.VISIBLE);
            doneButtonBadgeTextView.setText(String.format("%d", count));

            doneButtonTextView.setTag(Theme.key_picker_enabledButton);
            doneButtonTextView.setTextColor(Theme.getColor(Theme.key_picker_enabledButton));
            if (disable) {
                doneButton.setEnabled(true);
            }
        }
    }
}
