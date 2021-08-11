package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;

import java.util.ArrayList;

import kotlin.Unit;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.NekoXConfig;
import tw.nekomimi.nekogram.PopupBuilder;

@SuppressLint("RtlHardcoded")
public class NekoChatSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private ActionBarMenuItem menuItem;
    private StickerSizeCell stickerSizeCell;

    private int rowCount;

    private int stickerSizeHeaderRow;
    private int stickerSizeRow;
    private int stickerSize2Row;

    private int chatRow;
    private int ignoreBlockedRow;
    private int ignoreMutedCountRow;
    private int disableChatActionRow;
    private int disablePhotoSideActionRow;
    private int hideKeyboardOnChatScrollRow;
    private int disableVibrationRow;
    private int skipOpenLinkConfirmRow;
    private int rearVideoMessagesRow;
    private int confirmAVRow;
    private int useChatAttachMediaMenuRow;
    private int disableLinkPreviewByDefaultRow;
    private int sendCommentAfterForwardRow;
    private int disableProximityEventsRow;
    private int disableTrendingRow;
    private int dontSendGreetingStickerRow;
    private int hideTimeForStickerRow;
    private int takeGIFasVideoRow;

    private int mapPreviewRow;
    private int messageMenuRow;
    private int chat2Row;

    private int downloadRow;
    private int win32Row;
    private int archiveRow;
    private int download2Row;

    private int foldersRow;
    private int showTabsOnForwardRow;
    private int hideAllTabRow;
    private int pressTitleToOpenAllChatsRow;
    private int tabsTitleTypeRow;
    private int folders2Row;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

//        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiDidLoad);
        updateRows();

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("Chat", R.string.Chat));

        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }

        ActionBarMenu menu = actionBar.createMenu();
        menuItem = menu.addItem(0, R.drawable.ic_ab_other);
        menuItem.setContentDescription(LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
        menuItem.addSubItem(1, R.drawable.msg_reset, LocaleController.getString("ResetStickerSize", R.string.ResetStickerSize));
        menuItem.setVisibility(NekoConfig.stickerSize != 14.0f ? View.VISIBLE : View.GONE);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    NekoConfig.setStickerSize(14.0f);
                    menuItem.setVisibility(View.GONE);
                    stickerSizeCell.invalidate();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == ignoreBlockedRow) {
                NekoConfig.toggleIgnoreBlocked();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.ignoreBlocked);
                }
            } else if (position == mapPreviewRow) {

                PopupBuilder builder = new PopupBuilder(view);

                builder.setItems(new String[]{
                        LocaleController.getString("MapPreviewProviderTelegram", R.string.MapPreviewProviderTelegram),
                        LocaleController.getString("MapPreviewProviderYandex", R.string.MapPreviewProviderYandex),
                        LocaleController.getString("MapPreviewProviderNobody", R.string.MapPreviewProviderNobody)
                }, (i, __) -> {
                    NekoConfig.setMapPreviewProvider(i);
                    listAdapter.notifyItemChanged(position);
                    return Unit.INSTANCE;
                });

                builder.show();

            } else if (position == disableChatActionRow) {
                NekoConfig.toggleDisableChatAction();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableChatAction);
                }
            } else if (position == skipOpenLinkConfirmRow) {
                NekoConfig.toggleSkipOpenLinkConfirm();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.skipOpenLinkConfirm);
                }
            } else if (position == ignoreMutedCountRow) {
                NekoConfig.toggleIgnoredMutedCount();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.ignoreMutedCount);
                }
            } else if (position == messageMenuRow) {
                showMessageMenuAlert();
            } else if (position == disablePhotoSideActionRow) {
                NekoConfig.toggleDisablePhotoSideAction();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disablePhotoSideAction);
                }
            } else if (position == hideKeyboardOnChatScrollRow) {
                NekoConfig.toggleHideKeyboardOnChatScroll();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hideKeyboardOnChatScroll);
                }
            } else if (position == showTabsOnForwardRow) {
                NekoConfig.toggleShowTabsOnForward();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.showTabsOnForward);
                }
            } else if (position == rearVideoMessagesRow) {
                NekoConfig.toggleRearVideoMessages();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.rearVideoMessages);
                }
            } else if (position == hideAllTabRow) {
                NekoConfig.toggleHideAllTab();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hideAllTab);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            } else if (position == pressTitleToOpenAllChatsRow) {
                NekoConfig.togglePressTitleToOpenAllChats();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.pressTitleToOpenAllChats);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            } else if (position == tabsTitleTypeRow) {
                PopupBuilder builder = new PopupBuilder(view);

                builder.setItems(new String[]{

                        LocaleController.getString("TabTitleTypeText", R.string.TabTitleTypeText),
                        LocaleController.getString("TabTitleTypeIcon", R.string.TabTitleTypeIcon),
                        LocaleController.getString("TabTitleTypeMix", R.string.TabTitleTypeMix)

                }, (i, __) -> {

                    NekoConfig.setTabsTitleType(i);
                    listAdapter.notifyItemChanged(tabsTitleTypeRow);
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);

                    return Unit.INSTANCE;

                });

                builder.show();

            } else if (position == confirmAVRow) {
                NekoConfig.toggleConfirmAVMessage();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.confirmAVMessage);
                }
            } else if (position == useChatAttachMediaMenuRow) {
                NekoConfig.toggleUseChatAttachMediaMenu();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.useChatAttachMediaMenu);
                }
            } else if (position == disableLinkPreviewByDefaultRow) {
                NekoConfig.toggleDisableLinkPreviewByDefault();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableLinkPreviewByDefault);
                }
            } else if (position == sendCommentAfterForwardRow) {
                NekoConfig.toggleSendCommentAfterForward();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.sendCommentAfterForward);
                }
            } else if (position == disableVibrationRow) {
                NekoConfig.toggleDisableVibration();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableVibration);
                }
            } else if (position == disableProximityEventsRow) {
                NekoConfig.toggleDisableProximityEvents();
                MediaController.getInstance().recreateProximityWakeLock();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableProximityEvents);
                }
            } else if (position == disableTrendingRow) {
                NekoConfig.toggleDisableTrending();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableTrending);
                }
            } else if (position == dontSendGreetingStickerRow) {
                NekoConfig.toggleDontSendGreetingSticker();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.dontSendGreetingSticker);
                }
            } else if (position == hideTimeForStickerRow) {
                NekoConfig.toggleHideTimeForSticker();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hideTimeForSticker);
                }
            } else if (position == takeGIFasVideoRow) {
                NekoConfig.toggleTakeGIFasVideo();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.takeGIFasVideo);
                }
            } else if (position == win32Row) {
                NekoConfig.toggleDisableAutoDownloadingWin32Executable();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!NekoConfig.disableAutoDownloadingWin32Executable);
                }
            } else if (position == archiveRow) {
                NekoConfig.toggleDisableAutoDownloadingArchive();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(!NekoConfig.disableAutoDownloadingArchive);
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows() {
        rowCount = 0;
        stickerSizeHeaderRow = rowCount++;
        stickerSizeRow = rowCount++;
        stickerSize2Row = rowCount++;

        chatRow = rowCount++;
        ignoreBlockedRow = rowCount++;

        ignoreMutedCountRow = rowCount++;
        disableChatActionRow = rowCount++;

        disablePhotoSideActionRow = rowCount++;
        hideKeyboardOnChatScrollRow = rowCount++;
        disableVibrationRow = rowCount++;
        skipOpenLinkConfirmRow = rowCount++;
        rearVideoMessagesRow = rowCount++;
        confirmAVRow = rowCount++;
        useChatAttachMediaMenuRow = rowCount++;
        disableLinkPreviewByDefaultRow = rowCount++;
        sendCommentAfterForwardRow = rowCount++;
        disableProximityEventsRow = rowCount++;
        disableTrendingRow = rowCount++;
        dontSendGreetingStickerRow = rowCount++;
        hideTimeForStickerRow = rowCount++;
        takeGIFasVideoRow = rowCount++;

        mapPreviewRow = rowCount++;
        messageMenuRow = rowCount++;
        chat2Row = rowCount++;

        downloadRow = rowCount++;
        win32Row = rowCount++;
        archiveRow = rowCount++;
        download2Row = rowCount++;

        foldersRow = rowCount++;
        showTabsOnForwardRow = rowCount++;
        hideAllTabRow = rowCount++;
        pressTitleToOpenAllChatsRow = rowCount++;
        tabsTitleTypeRow = rowCount++;
        folders2Row = rowCount++;

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{EmptyCell.class, TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, TextDetailSettingsCell.class, NotificationsCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_avatar_actionBarIconBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_avatar_actionBarSelectorBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        return themeDescriptions;
    }

    private void showMessageMenuAlert() {
        if (getParentActivity() == null) {
            return;
        }
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("MessageMenu", R.string.MessageMenu));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout linearLayoutInviteContainer = new LinearLayout(context);
        linearLayoutInviteContainer.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayoutInviteContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int count = NekoXConfig.developerMode ? 10 : 9;
        for (int a = 0; a < count; a++) {
            TextCheckCell textCell = new TextCheckCell(context);
            switch (a) {
                case 0: {
                    textCell.setTextAndCheck(LocaleController.getString("DeleteDownloadedFile", R.string.DeleteDownloadedFile), NekoConfig.showDeleteDownloadedFile, false);
                    break;
                }
                case 1: {
                    textCell.setTextAndCheck(LocaleController.getString("AddToSavedMessages", R.string.AddToSavedMessages), NekoConfig.showAddToSavedMessages, false);
                    break;
                }
                case 2: {
                    textCell.setTextAndCheck(LocaleController.getString("Repeat", R.string.Repeat), NekoConfig.showRepeat, false);
                    break;
                }
                case 3: {
                    textCell.setTextAndCheck(LocaleController.getString("ViewHistory", R.string.ViewHistory), NekoConfig.showViewHistory, false);
                    break;
                }
                case 4: {
                    textCell.setTextAndCheck(LocaleController.getString("Translate", R.string.Translate), NekoConfig.showTranslate, false);
                    break;
                }
                case 5: {
                    textCell.setTextAndCheck(LocaleController.getString("ReportChat", R.string.ReportChat), NekoConfig.showReport, false);
                    break;
                }
                case 6: {
                    textCell.setTextAndCheck(LocaleController.getString("EditAdminRights", R.string.EditAdminRights), NekoConfig.showAdminActions, false);
                    break;
                }
                case 7: {
                    textCell.setTextAndCheck(LocaleController.getString("ChangePermissions", R.string.ChangePermissions), NekoConfig.showChangePermissions, false);
                    break;
                }
                case 8: {
                    textCell.setTextAndCheck(LocaleController.getString("Hide", R.string.Hide), NekoConfig.showMessageHide, false);
                    break;
                }
                case 9: {
                    textCell.setTextAndCheck(LocaleController.getString("MessageDetails", R.string.MessageDetails), NekoConfig.showMessageDetails, false);
                    break;
                }
            }
            textCell.setTag(a);
            textCell.setBackground(Theme.getSelectorDrawable(false));
            linearLayoutInviteContainer.addView(textCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            textCell.setOnClickListener(v2 -> {
                Integer tag = (Integer) v2.getTag();
                switch (tag) {
                    case 0: {
                        NekoConfig.toggleShowDeleteDownloadedFile();
                        textCell.setChecked(NekoConfig.showDeleteDownloadedFile);
                        break;
                    }
                    case 1: {
                        NekoConfig.toggleShowAddToSavedMessages();
                        textCell.setChecked(NekoConfig.showAddToSavedMessages);
                        break;
                    }
                    case 2: {
                        NekoConfig.toggleShowRepeat();
                        textCell.setChecked(NekoConfig.showRepeat);
                        break;
                    }
                    case 3: {
                        NekoConfig.toggleShowViewHistory();
                        textCell.setChecked(NekoConfig.showViewHistory);
                        break;
                    }
                    case 4: {
                        NekoConfig.toggleShowTranslate();
                        textCell.setChecked(NekoConfig.showTranslate);
                        break;
                    }
                    case 5: {
                        NekoConfig.toggleShowReport();
                        textCell.setChecked(NekoConfig.showReport);
                        break;
                    }
                    case 6: {
                        NekoConfig.toggleShowAdminActions();
                        textCell.setChecked(NekoConfig.showAdminActions);
                        break;
                    }
                    case 7: {
                        NekoConfig.toggleShowChangePermissions();
                        textCell.setChecked(NekoConfig.showChangePermissions);
                        break;
                    }
                    case 8: {
                        NekoConfig.toggleShowHide();
                        textCell.setChecked(NekoConfig.showMessageHide);
                        break;
                    }
                    case 9: {
                        NekoConfig.toggleShowMessageDetails();
                        textCell.setChecked(NekoConfig.showMessageDetails);
                        break;
                    }
                }
            });
        }
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        builder.setView(linearLayout);
        showDialog(builder.create());
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
       /* if (id == NotificationCenter.emojiDidLoad) {
            if (listView != null) {
                listView.invalidateViews();
            }
        }*/
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
//        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiDidLoad);
    }

    private class StickerSizeCell extends FrameLayout {

        private StickerSizePreviewMessagesCell messagesCell;
        private SeekBarView sizeBar;
        private int startStickerSize = 2;
        private int endStickerSize = 20;

        private TextPaint textPaint;

        public StickerSizeCell(Context context) {
            super(context);

            setWillNotDraw(false);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(AndroidUtilities.dp(16));

            sizeBar = new SeekBarView(context);
            sizeBar.setReportChanges(true);
            sizeBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    NekoConfig.setStickerSize(startStickerSize + (endStickerSize - startStickerSize) * progress);
                    StickerSizeCell.this.invalidate();
                    menuItem.setVisibility(View.VISIBLE);
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {

                }
            });
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 9, 5, 43, 11));

            messagesCell = new StickerSizePreviewMessagesCell(context, parentLayout);
            addView(messagesCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 53, 0, 0));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            canvas.drawText("" + Math.round(NekoConfig.stickerSize), getMeasuredWidth() - AndroidUtilities.dp(39), AndroidUtilities.dp(28), textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            sizeBar.setProgress((NekoConfig.stickerSize - startStickerSize) / (float) (endStickerSize - startStickerSize));
        }

        @Override
        public void invalidate() {
            super.invalidate();
            messagesCell.invalidate();
            sizeBar.invalidate();
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    if (position == folders2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == mapPreviewRow) {
                        String value;
                        switch (NekoConfig.mapPreviewProvider) {
                            case 0:
                                value = LocaleController.getString("MapPreviewProviderTelegram", R.string.MapPreviewProviderTelegram);
                                break;
                            case 1:
                                value = LocaleController.getString("MapPreviewProviderYandex", R.string.MapPreviewProviderYandex);
                                break;
                            case 2:
                            default:
                                value = LocaleController.getString("MapPreviewProviderNobody", R.string.MapPreviewProviderNobody);
                        }
                        textCell.setTextAndValue(LocaleController.getString("MapPreviewProvider", R.string.MapPreviewProvider), value, true);
                    } else if (position == stickerSizeRow) {
                        textCell.setTextAndValue(LocaleController.getString("StickerSize", R.string.StickerSize), String.valueOf(Math.round(NekoConfig.stickerSize)), true);
                    } else if (position == messageMenuRow) {
                        textCell.setText(LocaleController.getString("MessageMenu", R.string.MessageMenu), false);
                    } else if (position == tabsTitleTypeRow) {
                        String value;
                        switch (NekoConfig.tabsTitleType) {
                            case NekoConfig.TITLE_TYPE_TEXT:
                                value = LocaleController.getString("TabTitleTypeText", R.string.TabTitleTypeText);
                                break;
                            case NekoConfig.TITLE_TYPE_ICON:
                                value = LocaleController.getString("TabTitleTypeIcon", R.string.TabTitleTypeIcon);
                                break;
                            case NekoConfig.TITLE_TYPE_MIX:
                            default:
                                value = LocaleController.getString("TabTitleTypeMix", R.string.TabTitleTypeMix);
                        }
                        textCell.setTextAndValue(LocaleController.getString("TabTitleType", R.string.TabTitleType), value, false);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == ignoreBlockedRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("IgnoreBlocked", R.string.IgnoreBlocked), LocaleController.getString("IgnoreBlockedAbout", R.string.IgnoreBlockedAbout), NekoConfig.ignoreBlocked, true, true);
                    } else if (position == ignoreMutedCountRow) {
                        textCell.setTextAndCheck(LocaleController.getString("IgnoreMutedCount", R.string.IgnoreMutedCount), NekoConfig.ignoreMutedCount, true);
                    } else if (position == skipOpenLinkConfirmRow) {
                        textCell.setTextAndCheck(LocaleController.getString("SkipOpenLinkConfirm", R.string.SkipOpenLinkConfirm), NekoConfig.skipOpenLinkConfirm, true);
                    } else if (position == disableChatActionRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableChatAction", R.string.DisableChatAction), NekoConfig.disableChatAction, true);
                    } else if (position == disablePhotoSideActionRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisablePhotoViewerSideAction", R.string.DisablePhotoViewerSideAction), NekoConfig.disablePhotoSideAction, true);
                    } else if (position == hideKeyboardOnChatScrollRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HideKeyboardOnChatScroll", R.string.HideKeyboardOnChatScroll), NekoConfig.hideKeyboardOnChatScroll, true);
                    } else if (position == showTabsOnForwardRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ShowTabsOnForward", R.string.ShowTabsOnForward), NekoConfig.showTabsOnForward, true);
                    } else if (position == rearVideoMessagesRow) {
                        textCell.setTextAndCheck(LocaleController.getString("RearVideoMessages", R.string.RearVideoMessages), NekoConfig.rearVideoMessages, true);
                    } else if (position == hideAllTabRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("HideAllTab", R.string.HideAllTab), LocaleController.getString("HideAllTabAbout", R.string.HideAllTabAbout), NekoConfig.hideAllTab, true, true);
                    } else if(position == pressTitleToOpenAllChatsRow){
                        textCell.setTextAndCheck(LocaleController.getString("pressTitleToOpenAllChats", R.string.pressTitleToOpenAllChats),NekoConfig.pressTitleToOpenAllChats, true);
                    } else if (position == confirmAVRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ConfirmAVMessage", R.string.ConfirmAVMessage), NekoConfig.confirmAVMessage, true);
                    } else if (position == useChatAttachMediaMenuRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("UseChatAttachEnterMenu", R.string.UseChatAttachEnterMenu), LocaleController.getString("UseChatAttachEnterMenuNotice", R.string.UseChatAttachEnterMenuNotice), NekoConfig.useChatAttachMediaMenu, true, true);
                    } else if (position == disableLinkPreviewByDefaultRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("DisableLinkPreviewByDefault", R.string.DisableLinkPreviewByDefault), LocaleController.getString("DisableLinkPreviewByDefaultNotice", R.string.DisableLinkPreviewByDefaultNotice), NekoConfig.disableLinkPreviewByDefault, true, true);
                    } else if (position == sendCommentAfterForwardRow) {
                        textCell.setTextAndCheck(LocaleController.getString("SendCommentAfterForward", R.string.SendCommentAfterForward), NekoConfig.sendCommentAfterForward, true);
                    } else if (position == disableVibrationRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableVibration", R.string.DisableVibration), NekoConfig.disableVibration, true);
                    } else if (position == disableProximityEventsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableProximityEvents", R.string.DisableProximityEvents), NekoConfig.disableProximityEvents, true);
                    } else if (position == disableTrendingRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableTrending", R.string.DisableTrending), NekoConfig.disableTrending, true);
                    } else if (position == dontSendGreetingStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DontSendGreetingSticker", R.string.DontSendGreetingSticker), NekoConfig.dontSendGreetingSticker, true);
                    } else if (position == hideTimeForStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HideTimeForSticker", R.string.HideTimeForSticker), NekoConfig.hideTimeForSticker, true);
                    } else if (position == takeGIFasVideoRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TakeGIFasVideo", R.string.TakeGIFasVideo), NekoConfig.takeGIFasVideo, true);
                    } else if (position == win32Row) {
                        textCell.setTextAndCheck(LocaleController.getString("Win32ExecutableFiles", R.string.Win32ExecutableFiles), !NekoConfig.disableAutoDownloadingWin32Executable, true);
                    } else if (position == archiveRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ArchiveFiles", R.string.ArchiveFiles), !NekoConfig.disableAutoDownloadingArchive, false);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == chatRow) {
                        headerCell.setText(LocaleController.getString("Chat", R.string.Chat));
                    } else if (position == foldersRow) {
                        headerCell.setText(LocaleController.getString("Folder", R.string.Folder));
                    } else if (position == downloadRow) {
                        headerCell.setText(LocaleController.getString("AutoDownload", R.string.AutoDownload));
                    } else if (position == stickerSizeHeaderRow) {
                        headerCell.setText(LocaleController.getString("StickerSize", R.string.StickerSize));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case 1:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 2:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 5:
                    view = new NotificationsCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 6:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 7:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 8:
                    view = stickerSizeCell = new StickerSizeCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == chat2Row || position == folders2Row || position == download2Row || position == stickerSize2Row) {
                return 1;
            } else if (position == mapPreviewRow || position == messageMenuRow || position == tabsTitleTypeRow) {
                return 2;
            } else if (position == chatRow || position == foldersRow || position == downloadRow || position == stickerSizeHeaderRow) {
                return 4;
            } else if (position == stickerSizeRow) {
                return 8;
            }
            return 3;
        }
    }
}
