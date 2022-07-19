/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;
import tw.nekomimi.nekogram.ui.SuperTextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.EmojiTextView;
import org.telegram.ui.Components.GroupCreateCheckBox;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumGradient;

public class DrawerUserCell extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private SimpleTextView textView;
    private BackupImageView imageView;
    private AvatarDrawable avatarDrawable;
    private GroupCreateCheckBox checkBox;

    private int accountNumber;
    private RectF rect = new RectF();

    public DrawerUserCell(Context context) {
        super(context);

        avatarDrawable = new AvatarDrawable();
        avatarDrawable.setTextSize(AndroidUtilities.dp(12));

        imageView = new BackupImageView(context);
        imageView.setRoundRadius(AndroidUtilities.dp(18));
        addView(imageView, LayoutHelper.createFrame(36, 36, Gravity.LEFT | Gravity.TOP, 14, 6, 0, 0));

        textView = new SimpleTextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
        textView.setTextSize(15);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setMaxLines(1);
        textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 72, 0, 60, 0));

        checkBox = new GroupCreateCheckBox(context);
        checkBox.setChecked(true, false);
        checkBox.setCheckScale(0.9f);
        checkBox.setInnerRadDiff(AndroidUtilities.dp(1.5f));
        checkBox.setColorKeysOverrides(Theme.key_chats_unreadCounterText, Theme.key_chats_unreadCounter, Theme.key_chats_menuBackground);
        addView(checkBox, LayoutHelper.createFrame(18, 18, Gravity.LEFT | Gravity.TOP, 37, 27, 0, 0));

        setWillNotDraw(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        textView.setTextColor(Theme.getColor(Theme.key_chats_menuItemText));
        for (int i : SharedConfig.activeAccounts) {
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.currentUserPremiumStatusChanged);
        }
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (int i : SharedConfig.activeAccounts)  {
            NotificationCenter.getInstance(i).removeObserver(this, NotificationCenter.currentUserPremiumStatusChanged);
        }
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.currentUserPremiumStatusChanged) {
            if (account == accountNumber) {
                setAccount(accountNumber);
            }
        } else if (id == NotificationCenter.emojiLoaded) {
            textView.invalidate();
        }
    }

    public void setAccount(int account) {
        accountNumber = account;
        TLRPC.User user = UserConfig.getInstance(accountNumber).getCurrentUser();
        if (user == null) {
            return;
        }
        avatarDrawable.setInfo(user);
        CharSequence text = ContactsController.formatName(user.first_name, user.last_name);
        try {
            text = Emoji.replaceEmoji(text, textView.getPaint().getFontMetricsInt(), AndroidUtilities.dp(20), false);
        } catch (Exception ignore) {}
        textView.setText(text);
        if (MessagesController.getInstance(account).isPremiumUser(user)) {
            textView.setDrawablePadding(AndroidUtilities.dp(6));
            textView.setRightDrawable(PremiumGradient.getInstance().premiumStarDrawableMini);
        } else {
            textView.setRightDrawable(null);
        }
        imageView.getImageReceiver().setCurrentAccount(account);
        imageView.setForUserOrChat(user, avatarDrawable);
        checkBox.setVisibility(account == UserConfig.selectedAccount ? VISIBLE : INVISIBLE);
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (UserConfig.getActivatedAccountsCount() <= 1 || !NotificationsController.getInstance(accountNumber).showBadgeNumber) {
            return;
        }
        int counter = MessagesStorage.getInstance(accountNumber).getMainUnreadCount();
        if (counter <= 0) {
            return;
        }

        String text = String.format("%d", counter);
        int countTop = AndroidUtilities.dp(12.5f);
        int textWidth = (int) Math.ceil(Theme.dialogs_countTextPaint.measureText(text));
        int countWidth = Math.max(AndroidUtilities.dp(10), textWidth);
        int countLeft = getMeasuredWidth() - countWidth - AndroidUtilities.dp(25);

        int x = countLeft - AndroidUtilities.dp(5.5f);
        rect.set(x, countTop, x + countWidth + AndroidUtilities.dp(14), countTop + AndroidUtilities.dp(23));
        canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, Theme.dialogs_countPaint);

        canvas.drawText(text, rect.left + (rect.width() - textWidth) / 2, countTop + AndroidUtilities.dp(16), Theme.dialogs_countTextPaint);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
    }
}
