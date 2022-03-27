package org.telegram.ui.Components;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.SharedAudioCell;
import org.telegram.ui.Cells.SharedDocumentCell;
import org.telegram.ui.FilteredSearchView;
import org.telegram.ui.PhotoViewer;

import java.util.ArrayList;

public class SearchDownloadsContainer extends FrameLayout implements NotificationCenter.NotificationCenterDelegate {

    private final FlickerLoadingView loadingView;
    StickerEmptyView emptyView;
    public RecyclerListView recyclerListView;
    DownloadsAdapter adapter = new DownloadsAdapter();
    private final int currentAccount;

    ArrayList<MessageObject> currentLoadingFiles = new ArrayList<>();
    ArrayList<MessageObject> recentLoadingFiles = new ArrayList<>();

    ArrayList<MessageObject> currentLoadingFilesTmp = new ArrayList<>();
    ArrayList<MessageObject> recentLoadingFilesTmp = new ArrayList<>();

    int rowCount;
    int downloadingFilesHeader = -1;
    int downloadingFilesStartRow = -1;
    int downloadingFilesEndRow = -1;
    int recentFilesHeader = -1;
    int recentFilesStartRow = -1;
    int recentFilesEndRow = -1;

    Activity parentActivity;
    BaseFragment parentFragment;
    private boolean hasCurrentDownload;

    FilteredSearchView.UiCallback uiCallback;
    private final FilteredSearchView.MessageHashId messageHashIdTmp = new FilteredSearchView.MessageHashId(0, 0);
    String searchQuery;
    String lastQueryString;
    Runnable lastSearchRunnable;
    RecyclerItemsEnterAnimator itemsEnterAnimator;

    public SearchDownloadsContainer(BaseFragment fragment, int currentAccount) {
        super(fragment.getParentActivity());
        this.parentFragment = fragment;
        this.parentActivity = fragment.getParentActivity();
        this.currentAccount = currentAccount;
        recyclerListView = new BlurredRecyclerView(getContext());
        addView(recyclerListView);
        recyclerListView.setLayoutManager(new LinearLayoutManager(fragment.getParentActivity()) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        });
        recyclerListView.setAdapter(adapter);
        recyclerListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(parentActivity.getCurrentFocus());
                }
            }
        });
        DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator();
        defaultItemAnimator.setDelayAnimations(false);
        defaultItemAnimator.setSupportsChangeAnimations(false);
        recyclerListView.setItemAnimator(defaultItemAnimator);

        recyclerListView.setOnItemClickListener((view, position) -> {
            MessageObject messageObject = adapter.getMessage(position);
            if (messageObject == null) {
                return;
            }
            if (uiCallback.actionModeShowing()) {
                uiCallback.toggleItemSelection(messageObject, view, 0);
                messageHashIdTmp.set(messageObject.getId(), messageObject.getDialogId());
                adapter.notifyItemChanged(position);
                return;
            }

            if (view instanceof Cell) {
                SharedDocumentCell cell = ((Cell) view).sharedDocumentCell;
                MessageObject message = cell.getMessage();
                TLRPC.Document document = message.getDocument();
                if (cell.isLoaded()) {
                    if (message.isRoundVideo() || message.isVoice()) {
                        MediaController.getInstance().playMessage(message);
                        return;
                    }
                    if (message.canPreviewDocument()) {
                        PhotoViewer.getInstance().setParentActivity(parentActivity);

                        ArrayList<MessageObject> documents = new ArrayList<>();
                        documents.add(message);
                        PhotoViewer.getInstance().setParentActivity(parentActivity);
                        PhotoViewer.getInstance().openPhoto(documents, 0, 0, 0, new PhotoViewer.EmptyPhotoViewerProvider());
                        return;
                    }
                    AndroidUtilities.openDocument(message, parentActivity, parentFragment);
                } else if (!cell.isLoading()) {
                    messageObject.putInDownloadsStore = true;
                    AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().loadFile(document, messageObject, 0, 0);
                    cell.updateFileExistIcon(true);
                } else {
                    AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().cancelLoadFile(document);
                    cell.updateFileExistIcon(true);
                }
                update(true);
            }
            if (view instanceof SharedAudioCell) {
                SharedAudioCell cell = (SharedAudioCell) view;
                cell.didPressedButton();
            }
        });
        recyclerListView.setOnItemLongClickListener((view, position) -> {
            MessageObject messageObject = adapter.getMessage(position);
            if (messageObject != null) {
                if (!uiCallback.actionModeShowing()) {
                    uiCallback.showActionMode();
                }
                if (uiCallback.actionModeShowing()) {
                    uiCallback.toggleItemSelection(messageObject, view, 0);
                    messageHashIdTmp.set(messageObject.getId(), messageObject.getDialogId());
                    adapter.notifyItemChanged(position);
                }
                return true;
            }
            return false;
        });
        itemsEnterAnimator = new RecyclerItemsEnterAnimator(recyclerListView, true);


        addView(loadingView = new FlickerLoadingView(getContext()));
        loadingView.setUseHeaderOffset(true);
        loadingView.setViewType(FlickerLoadingView.FILES_TYPE);
        loadingView.setVisibility(View.GONE);
        emptyView = new StickerEmptyView(getContext(), loadingView, StickerEmptyView.STICKER_TYPE_SEARCH);
        addView(emptyView);
        recyclerListView.setEmptyView(emptyView);

        FileLoader.getInstance(currentAccount).getCurrentLoadingFiles(currentLoadingFiles);
        update(false);
    }

    public void update(boolean animated) {
        if (TextUtils.isEmpty(searchQuery) || isEmptyDownloads()) {
            if (rowCount == 0) {
                itemsEnterAnimator.showItemsAnimated(0);
            }
            FileLoader.getInstance(currentAccount).getCurrentLoadingFiles(currentLoadingFilesTmp);
            FileLoader.getInstance(currentAccount).getRecentLoadingFiles(recentLoadingFilesTmp);

            for (int i = 0; i < currentLoadingFiles.size(); i++) {
                currentLoadingFiles.get(i).setQuery(null);
            }
            for (int i = 0; i < recentLoadingFiles.size(); i++) {
                recentLoadingFiles.get(i).setQuery(null);
            }

            lastQueryString = null;
            updateListInternal(animated, currentLoadingFilesTmp, recentLoadingFilesTmp);
            if (rowCount == 0) {
                emptyView.showProgress(false, false);
                emptyView.title.setText(LocaleController.getString("SearchEmptyViewDownloads", R.string.SearchEmptyViewDownloads));
                emptyView.subtitle.setVisibility(View.GONE);
            }
            emptyView.setStickerType(9);
        } else {
            emptyView.setStickerType(1);
            ArrayList<MessageObject> currentLoadingFilesTmp = new ArrayList<>();
            ArrayList<MessageObject> recentLoadingFilesTmp = new ArrayList<>();

            FileLoader.getInstance(currentAccount).getCurrentLoadingFiles(currentLoadingFilesTmp);
            FileLoader.getInstance(currentAccount).getRecentLoadingFiles(recentLoadingFilesTmp);

            String q = searchQuery.toLowerCase();
            boolean sameQuery = q.equals(lastQueryString);

            lastQueryString = q;
            Utilities.searchQueue.cancelRunnable(lastSearchRunnable);
            Utilities.searchQueue.postRunnable(lastSearchRunnable = () -> {
                ArrayList<MessageObject> currentLoadingFilesRes = new ArrayList<>();
                ArrayList<MessageObject> recentLoadingFilesRes = new ArrayList<>();
                for (int i = 0; i < currentLoadingFilesTmp.size(); i++) {
                    if (FileLoader.getDocumentFileName(currentLoadingFilesTmp.get(i).getDocument()).toLowerCase().contains(q)) {
                        MessageObject messageObject = new MessageObject(currentAccount, currentLoadingFilesTmp.get(i).messageOwner, false, false);
                        messageObject.mediaExists = currentLoadingFilesTmp.get(i).mediaExists;
                        messageObject.setQuery(searchQuery);
                        currentLoadingFilesRes.add(messageObject);
                    }
                }

                for (int i = 0; i < recentLoadingFilesTmp.size(); i++) {
                    if (FileLoader.getDocumentFileName(recentLoadingFilesTmp.get(i).getDocument()).toLowerCase().contains(q)) {
                        MessageObject messageObject = new MessageObject(currentAccount, recentLoadingFilesTmp.get(i).messageOwner, false, false);
                        messageObject.mediaExists = recentLoadingFilesTmp.get(i).mediaExists;
                        messageObject.setQuery(searchQuery);
                        recentLoadingFilesRes.add(messageObject);
                    }
                }
                AndroidUtilities.runOnUIThread(() -> {
                    if (q.equals(lastQueryString)) {
                        if (rowCount == 0) {
                            itemsEnterAnimator.showItemsAnimated(0);
                        }
                        updateListInternal(true, currentLoadingFilesRes, recentLoadingFilesRes);
                        if (rowCount == 0) {
                            emptyView.showProgress(false, true);

                            emptyView.title.setText(LocaleController.getString("SearchEmptyViewTitle2", R.string.SearchEmptyViewTitle2));
                            emptyView.subtitle.setVisibility(View.VISIBLE);
                            emptyView.subtitle.setText(LocaleController.getString("SearchEmptyViewFilteredSubtitle2", R.string.SearchEmptyViewFilteredSubtitle2));
                        }
                    }
                });

            }, sameQuery ? 0 : 300);

            this.recentLoadingFilesTmp.clear();
            this.currentLoadingFilesTmp.clear();
            if (!sameQuery) {
                emptyView.showProgress(true, true);
                updateListInternal(animated, this.currentLoadingFilesTmp, this.recentLoadingFilesTmp);
            }
        }
    }

    private boolean isEmptyDownloads() {
        return DownloadController.getInstance(currentAccount).downloadingFiles.isEmpty() && DownloadController.getInstance(currentAccount).recentDownloadingFiles.isEmpty();
    }

    private void updateListInternal(boolean animated, ArrayList<MessageObject> currentLoadingFilesTmp, ArrayList<MessageObject> recentLoadingFilesTmp) {
        if (animated) {
            int oldDownloadingFilesHeader = downloadingFilesHeader;
            int oldDownloadingFilesStartRow = downloadingFilesStartRow;
            int oldDownloadingFilesEndRow = downloadingFilesEndRow;

            int oldRecentFilesHeader = recentFilesHeader;
            int oldRecentFilesStartRow = recentFilesStartRow;
            int oldRecentFilesEndRow = recentFilesEndRow;

            int oldRowCount = rowCount;

            ArrayList<MessageObject> oldDownloadingLoadingFiles = new ArrayList<>(currentLoadingFiles);
            ArrayList<MessageObject> oldRecentLoadingFiles = new ArrayList<>(recentLoadingFiles);

            updateRows(currentLoadingFilesTmp, recentLoadingFilesTmp);
            DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldRowCount;
                }

                @Override
                public int getNewListSize() {
                    return rowCount;
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    if (oldItemPosition >= 0 && newItemPosition >= 0) {
                        if (oldItemPosition == oldDownloadingFilesHeader && newItemPosition == downloadingFilesHeader) {
                            return true;
                        }
                        if (oldItemPosition == oldRecentFilesHeader && newItemPosition == recentFilesHeader) {
                            return true;
                        }
                    }
                    MessageObject oldItem = null;
                    MessageObject newItem = null;

                    if (oldItemPosition >= oldDownloadingFilesStartRow && oldItemPosition < oldDownloadingFilesEndRow) {
                        oldItem = oldDownloadingLoadingFiles.get(oldItemPosition - oldDownloadingFilesStartRow);
                    } else if (oldItemPosition >= oldRecentFilesStartRow && oldItemPosition < oldRecentFilesEndRow) {
                        oldItem = oldRecentLoadingFiles.get(oldItemPosition - oldRecentFilesStartRow);
                    }

                    if (newItemPosition >= downloadingFilesStartRow && newItemPosition < downloadingFilesEndRow) {
                        newItem = currentLoadingFiles.get(newItemPosition - downloadingFilesStartRow);
                    } else if (newItemPosition >= recentFilesStartRow && newItemPosition < recentFilesEndRow) {
                        newItem = recentLoadingFiles.get(newItemPosition - recentFilesStartRow);
                    }
                    if (newItem != null && oldItem != null) {
                        return newItem.getDocument().id == oldItem.getDocument().id;
                    }
                    return false;
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return areItemsTheSame(oldItemPosition, newItemPosition);
                }
            }).dispatchUpdatesTo(adapter);
            for (int i = 0; i < recyclerListView.getChildCount(); i++) {
                View child = recyclerListView.getChildAt(i);
                int p = recyclerListView.getChildAdapterPosition(child);
                if (p >= 0) {
                    RecyclerView.ViewHolder holder = recyclerListView.getChildViewHolder(child);
                    if (holder == null || holder.shouldIgnore()) {
                        continue;
                    }
                    if (child instanceof GraySectionCell) {
                        adapter.onBindViewHolder(holder, p);
                    } else if (child instanceof Cell) {
                        Cell cell = (Cell) child;
                        cell.sharedDocumentCell.updateFileExistIcon(true);
                        messageHashIdTmp.set(cell.sharedDocumentCell.getMessage().getId(), cell.sharedDocumentCell.getMessage().getDialogId());
                        cell.sharedDocumentCell.setChecked(uiCallback.isSelected(messageHashIdTmp), true);
                    }
                }
            }
        } else {
            updateRows(currentLoadingFilesTmp, recentLoadingFilesTmp);
            adapter.notifyDataSetChanged();
        }
    }

    private void updateRows(ArrayList<MessageObject> currentLoadingFilesTmp, ArrayList<MessageObject> recentLoadingFilesTmp) {
        currentLoadingFiles.clear();
        currentLoadingFiles.addAll(currentLoadingFilesTmp);

        recentLoadingFiles.clear();
        recentLoadingFiles.addAll(recentLoadingFilesTmp);

        rowCount = 0;
        downloadingFilesHeader = -1;
        downloadingFilesStartRow = -1;
        downloadingFilesEndRow = -1;
        recentFilesHeader = -1;
        recentFilesStartRow = -1;
        recentFilesEndRow = -1;
        hasCurrentDownload = false;

        if (!currentLoadingFiles.isEmpty()) {
            downloadingFilesHeader = rowCount++;
            downloadingFilesStartRow = rowCount;
            rowCount += currentLoadingFiles.size();
            downloadingFilesEndRow = rowCount;

            for (int i = 0; i < currentLoadingFiles.size(); i++) {
                if (FileLoader.getInstance(currentAccount).isLoadingFile(currentLoadingFiles.get(i).getFileName())) {
                    hasCurrentDownload = true;
                    break;
                }
            }
        }
        if (!recentLoadingFiles.isEmpty()) {
            recentFilesHeader = rowCount++;
            recentFilesStartRow = rowCount;
            rowCount += recentLoadingFiles.size();
            recentFilesEndRow = rowCount;

        }
    }

    public void search(String query) {
        searchQuery = query;
        update(false);
    }

    private class DownloadsAdapter extends RecyclerListView.SelectionAdapter {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == 0) {
                view = new GraySectionCell(parent.getContext());
            } else if (viewType == 1){
                Cell sharedDocumentCell = new Cell(parent.getContext());
                view = sharedDocumentCell;
            } else {
                SharedAudioCell sharedAudioCell = new SharedAudioCell(parent.getContext()) {
                    @Override
                    public boolean needPlayMessage(MessageObject messageObject) {
                        return MediaController.getInstance().playMessage(messageObject);
                    }
                };
                view = sharedAudioCell;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);

        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int type = holder.getItemViewType();
            if (type == 0) {
                GraySectionCell graySectionCell = (GraySectionCell) holder.itemView;
                if (position == downloadingFilesHeader) {
                    graySectionCell.setText(LocaleController.getString("Downloading", R.string.Downloading), hasCurrentDownload ? LocaleController.getString("PauseAll", R.string.PauseAll) : LocaleController.getString("ResumeAll", R.string.ResumeAll), new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            for (int i = 0; i < currentLoadingFiles.size(); i++) {
                                MessageObject messageObject = currentLoadingFiles.get(i);
                                if (hasCurrentDownload) {
                                    AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().cancelLoadFile(messageObject.getDocument());
                                } else {
                                    AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().loadFile(messageObject.getDocument(), messageObject, 0, 0);
                                }
                            }
                            update(true);
                        }
                    });
                } else if (position == recentFilesHeader) {
                    graySectionCell.setText(LocaleController.getString("RecentlyDownloaded", R.string.RecentlyDownloaded), LocaleController.getString("Settings", R.string.Settings), new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showSettingsDialog();
                        }
                    });
                }
            } else {
                MessageObject messageObject = getMessage(position);
                if (messageObject != null) {
                    if (type == 1) {
                        Cell view = (Cell) holder.itemView;
                        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                        int oldId = view.sharedDocumentCell.getMessage() == null ? 0 : view.sharedDocumentCell.getMessage().getId();
                        view.sharedDocumentCell.setDocument(messageObject, true);
                        messageHashIdTmp.set(view.sharedDocumentCell.getMessage().getId(), view.sharedDocumentCell.getMessage().getDialogId());
                        view.sharedDocumentCell.setChecked(uiCallback.isSelected(messageHashIdTmp), oldId == messageObject.getId());
                    } else if (type == 2) {
                        SharedAudioCell sharedAudioCell = (SharedAudioCell) holder.itemView;
                        sharedAudioCell.setMessageObject(messageObject, true);
                        int oldId = sharedAudioCell.getMessage() == null ? 0 : sharedAudioCell.getMessage().getId();
                        sharedAudioCell.setChecked(uiCallback.isSelected(messageHashIdTmp), oldId == messageObject.getId());
                    }
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == downloadingFilesHeader || position == recentFilesHeader) {
                return 0;
            }
            MessageObject messageObject = getMessage(position);
            if (messageObject == null) {
                return 1;
            }
            if (messageObject.isMusic()) {
                return 2;
            }
            return 1;
        }

        private MessageObject getMessage(int position) {
            if (position >= downloadingFilesStartRow && position < downloadingFilesEndRow) {;
                return currentLoadingFiles.get(position - downloadingFilesStartRow);
            } else if (position >= recentFilesStartRow && position < recentFilesEndRow) {
                return recentLoadingFiles.get(position - recentFilesStartRow);
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == 1 || holder.getItemViewType() == 2;
        }
    }

    private void showSettingsDialog() {
        if (parentFragment == null || parentActivity == null) {
            return;
        }
        BottomSheet bottomSheet = new BottomSheet(parentActivity, false);
        Context context = parentFragment.getParentActivity();
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        StickerImageView imageView = new StickerImageView(context, currentAccount);
        imageView.setStickerNum(9);
        imageView.getImageReceiver().setAutoRepeat(1);
        linearLayout.addView(imageView, LayoutHelper.createLinear(144, 144, Gravity.CENTER_HORIZONTAL, 0, 16, 0, 0));

        TextView title = new TextView(context);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        title.setText(LocaleController.getString("DownloadedFiles", R.string.DownloadedFiles));
        linearLayout.addView(title, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 30, 21, 0));

        TextView description = new TextView(context);
        description.setGravity(Gravity.CENTER_HORIZONTAL);
        description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        description.setTextColor(Theme.getColor(Theme.key_dialogTextHint));
        description.setText(LocaleController.formatString("DownloadedFilesMessage", R.string.DownloadedFilesMessage));
        linearLayout.addView(description, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 15, 21, 16));


        TextView buttonTextView = new TextView(context);
        buttonTextView.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
        buttonTextView.setGravity(Gravity.CENTER);
        buttonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        buttonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        buttonTextView.setText(LocaleController.getString("ManageDeviceStorage", R.string.ManageDeviceStorage));

        buttonTextView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        buttonTextView.setBackgroundDrawable(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton), ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhite), 120)));

        linearLayout.addView(buttonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 16, 15, 16, 16));


        TextView buttonTextView2 = new TextView(context);
        buttonTextView2.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
        buttonTextView2.setGravity(Gravity.CENTER);
        buttonTextView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        buttonTextView2.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        buttonTextView2.setText(LocaleController.getString("ClearDownloadsList", R.string.ClearDownloadsList));

        buttonTextView2.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        buttonTextView2.setBackgroundDrawable(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Color.TRANSPARENT, ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_featuredStickers_addButton), 120)));

        linearLayout.addView(buttonTextView2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 16, 0, 16, 16));

        NestedScrollView scrollView = new NestedScrollView(context);
        scrollView.addView(linearLayout);
        bottomSheet.setCustomView(scrollView);
        bottomSheet.show();

        buttonTextView.setOnClickListener(view -> {
            bottomSheet.dismiss();
            if (parentFragment != null) {
                parentFragment.presentFragment(new CacheControlActivity());
            }
        });
        buttonTextView2.setOnClickListener(view -> {
            bottomSheet.dismiss();
            DownloadController.getInstance(currentAccount).clearRecentDownloadedFiles();
        });
        //parentFragment.showDialog(bottomSheet);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.onDownloadingFilesChanged);
        if (getVisibility() == View.VISIBLE) {
            DownloadController.getInstance(currentAccount).clearUnviewedDownloads();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.onDownloadingFilesChanged);
    }


    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.onDownloadingFilesChanged) {
            if (getVisibility() == View.VISIBLE) {
                DownloadController.getInstance(currentAccount).clearUnviewedDownloads();
            }
            update(true);
        }
    }

    private class Cell extends FrameLayout {

        SharedDocumentCell sharedDocumentCell;

        public Cell(@NonNull Context context) {
            super(context);
            sharedDocumentCell = new SharedDocumentCell(context, SharedDocumentCell.VIEW_TYPE_GLOBAL_SEARCH);
            sharedDocumentCell.rightDateTextView.setVisibility(View.GONE);
            addView(sharedDocumentCell);
        }
    }

    public void setUiCallback(FilteredSearchView.UiCallback callback) {
        this.uiCallback = callback;
    }

    public void setKeyboardHeight(int keyboardSize, boolean animated) {
        emptyView.setKeyboardHeight(keyboardSize, animated);
    }
}
