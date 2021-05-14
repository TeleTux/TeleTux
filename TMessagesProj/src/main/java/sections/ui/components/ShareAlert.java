/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package sections.ui.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import telegram.messenger.xtelex.ApplicationLoader;
import telegram.messenger.xtelex.R;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.support.widget.GridLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShareDialogCell;
import org.telegram.ui.Components.CheckBoxSquare;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import sections.ui.components.Switch;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class ShareAlert extends BottomSheet implements NotificationCenter.NotificationCenterDelegate {

    private FrameLayout frameLayout;
    private FrameLayout frameLayout2;
    private TextView doneButtonBadgeTextView;
    private TextView doneButtonTextView;
    private LinearLayout doneButton;
    private EditTextBoldCursor nameTextView;
    private EditTextBoldCursor commentTextView;
    private View shadow;
    private View shadow2;
    private AnimatorSet animatorSet;
    private RecyclerListView gridView;
    private GridLayoutManager layoutManager;
    private ShareDialogsAdapter listAdapter;
    private ShareSearchAdapter searchAdapter;
    private ArrayList<MessageObject> sendingMessageObjects;
    private String sendingText;
    private EmptyTextProgressView searchEmptyView;
    private Drawable shadowDrawable;
    private HashMap<Long, TLRPC.TL_dialog> selectedDialogs = new HashMap<>();

    private TLRPC.TL_exportedMessageLink exportedMessageLink;
    private boolean loadingLink;
    private boolean copyLinkOnEnd;

    private boolean isPublicChannel;
    private String linkToCopy;

    private int scrollOffsetY;
    private int topBeforeSwitch;

    // Tele X
    private Switch quoteSwitch;
    private boolean favsFirst;

    private CheckBoxSquare checkBox;
    private boolean checked = false;

    private FrameLayout tabsView;
    private LinearLayout tabsLayout;
    private int tabsHeight = 40;
    private ImageView allTab;
    private ImageView usersTab;
    private ImageView groupsTab;
    private ImageView superGroupsTab;
    private ImageView channelsTab;
    private ImageView botsTab;
    private ImageView favsTab;
    private ImageView contactsTab;

    private int dialogsType = 0;
    private DialogsOnTouch onTouchListener = null;
    private float touchPositionDP;

    private int currentAccount = UserConfig.selectedAccount;


    public static ShareAlert createShareAlert(final Context context, MessageObject messageObject, final String text, boolean publicChannel, final String copyLink, boolean fullScreen) {
        ArrayList<MessageObject> arrayList;
        if (messageObject != null) {
            arrayList = new ArrayList<>();
            arrayList.add(messageObject);
        } else {
            arrayList = null;
        }
        return new ShareAlert(context, arrayList, text, publicChannel, copyLink, fullScreen);
    }

    public ShareAlert(final Context context, ArrayList<MessageObject> messages, final String text, boolean publicChannel, final String copyLink, boolean fullScreen) {
        super(context, true);

        shadowDrawable = context.getResources().getDrawable(R.drawable.sheet_shadow).mutate();
        shadowDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogBackground), PorterDuff.Mode.MULTIPLY));

        linkToCopy = copyLink;
        sendingMessageObjects = messages;
        searchAdapter = new ShareSearchAdapter(context);
        isPublicChannel = publicChannel;
        sendingText = text;

        if (publicChannel) {
            loadingLink = true;
            TLRPC.TL_channels_exportMessageLink req = new TLRPC.TL_channels_exportMessageLink();
            req.id = messages.get(0).getId();
            req.channel = MessagesController.getInstance(currentAccount).getInputChannel(messages.get(0).messageOwner.to_id.channel_id);
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, new RequestDelegate() {
                @Override
                public void run(final TLObject response, TLRPC.TL_error error) {
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response != null) {
                                exportedMessageLink = (TLRPC.TL_exportedMessageLink) response;
                                if (copyLinkOnEnd) {
                                    copyLink(context);
                                }
                            }
                            loadingLink = false;
                        }
                    });
                }
            });
        }

        containerView = new FrameLayout(context) {

            private boolean ignoreLayout = false;

            @Override
            public boolean onInterceptTouchEvent(MotionEvent ev) {
                if (ev.getAction() == MotionEvent.ACTION_DOWN && scrollOffsetY != 0 && ev.getY() < scrollOffsetY) {
                    dismiss();
                    return true;
                }
                return super.onInterceptTouchEvent(ev);
            }

            @Override
            public boolean onTouchEvent(MotionEvent e) {
                return !isDismissed() && super.onTouchEvent(e);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int height = MeasureSpec.getSize(heightMeasureSpec);
                if (Build.VERSION.SDK_INT >= 21) {
                    height -= AndroidUtilities.statusBarHeight;
                }
                int size = Math.max(searchAdapter.getItemCount(), listAdapter.getItemCount());
                int contentSize = AndroidUtilities.dp(48) + Math.max(3, (int) Math.ceil(size / 4.0f)) * AndroidUtilities.dp(100) + backgroundPaddingTop;
                int padding = contentSize < height ? 0 : height - (height / 5 * 3) + AndroidUtilities.dp(8);
                if (gridView.getPaddingTop() != padding) {
                    ignoreLayout = true;
                    gridView.setPadding(0, padding, 0, AndroidUtilities.dp(frameLayout2.getTag() != null ? 56 : 8));
                    ignoreLayout = false;
                }
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(Math.min(contentSize, height), MeasureSpec.EXACTLY));
            }

            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                updateLayout();
            }

            @Override
            public void requestLayout() {
                if (ignoreLayout) {
                    return;
                }
                super.requestLayout();
            }

            @Override
            protected void onDraw(Canvas canvas) {
                shadowDrawable.setBounds(0, scrollOffsetY - backgroundPaddingTop, getMeasuredWidth(), getMeasuredHeight());
                shadowDrawable.draw(canvas);
            }
        };
        containerView.setWillNotDraw(false);
        containerView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);

        frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        frameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        doneButton = new LinearLayout(context);
        doneButton.setOrientation(LinearLayout.HORIZONTAL);
        doneButton.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), 0));
        doneButton.setPadding(AndroidUtilities.dp(21), 0, AndroidUtilities.dp(21), 0);
        frameLayout.addView(doneButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.RIGHT));
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DoneClicked();
            }
        });

        doneButtonBadgeTextView = new TextView(context);
        doneButtonBadgeTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        doneButtonBadgeTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        doneButtonBadgeTextView.setTextColor(Theme.getColor(Theme.key_dialogBadgeText));
        doneButtonBadgeTextView.setGravity(Gravity.CENTER);
        doneButtonBadgeTextView.setBackgroundDrawable(Theme.createRoundRectDrawable(AndroidUtilities.dp(12.5f), Theme.getColor(Theme.key_dialogBadgeBackground)));
        doneButtonBadgeTextView.setMinWidth(AndroidUtilities.dp(23));
        doneButtonBadgeTextView.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), AndroidUtilities.dp(1));
        doneButton.addView(doneButtonBadgeTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 23, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        doneButtonTextView = new TextView(context);
        doneButtonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        doneButtonTextView.setGravity(Gravity.CENTER);
        doneButtonTextView.setCompoundDrawablePadding(AndroidUtilities.dp(8));
        doneButtonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        doneButton.addView(doneButtonTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));


        checkBox = new CheckBoxSquare(context, false);
        checkBox.setVisibility(View.VISIBLE);
       // checkBox.setColor(0xff3ec1f9);
        checkBox.setChecked(checked, false);
        frameLayout.addView(checkBox, LayoutHelper.createFrame(18, 18, Gravity.LEFT | Gravity.CENTER_VERTICAL, 10, 0, 5, 0));

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                checked = !checked;
                checkBox.setChecked(checked, true);
                setCheckAll(checked);

            }
        });

        //switch
        final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("BaseConfig", Activity.MODE_PRIVATE);
        quoteSwitch = new Switch(context);
        quoteSwitch.setTag("chat");
        quoteSwitch.setDuplicateParentStateEnabled(false);
        quoteSwitch.setFocusable(false);
        quoteSwitch.setFocusableInTouchMode(false);
        quoteSwitch.setClickable(true);
        setCheck(preferences.getBoolean("directShareQuote", true));
        setCheckColor();
        frameLayout.addView(quoteSwitch, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 32, 2, 0, 0));
        /*quoteSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("directShareQuote", isChecked).apply();
                setCheckColor();
            }
        });*/

        TextView quoteTextView = new TextView(context);
        quoteTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
        //quoteTextView.setTextColor(0xff979797);
        quoteTextView.setTextColor(quoteTextView.getCurrentTextColor() != 0xff757575 ? 0xff757575 : 0xff979797);
        quoteTextView.setGravity(Gravity.CENTER);
        quoteTextView.setCompoundDrawablePadding(AndroidUtilities.dp(8));
        quoteTextView.setText(LocaleController.getString("Quote", R.string.Quote).toUpperCase());
        quoteTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        frameLayout.addView(quoteTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 44, 2, 0, 0));


        ImageView imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.ic_ab_search);
        imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogIcon), PorterDuff.Mode.MULTIPLY));
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        frameLayout.addView(imageView, LayoutHelper.createFrame(48, 48, Gravity.LEFT | Gravity.CENTER_VERTICAL, 80, 0, 0, 0));

        nameTextView = new EditTextBoldCursor(context);
        nameTextView.setHint(LocaleController.getString("ShareSendTo", R.string.ShareSendTo));
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        nameTextView.setBackgroundDrawable(null);
        nameTextView.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        nameTextView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        nameTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameTextView.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        nameTextView.setCursorSize(AndroidUtilities.dp(20));
        nameTextView.setCursorWidth(1.5f);
        nameTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        frameLayout.addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 128, 2, 96, 0));
        nameTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = nameTextView.getText().toString();
                if (text.length() != 0) {
                    if (gridView.getAdapter() != searchAdapter) {
                        topBeforeSwitch = getCurrentTop();
                        gridView.setAdapter(searchAdapter);
                        searchAdapter.notifyDataSetChanged();
                    }
                    if (searchEmptyView != null) {
                        searchEmptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
                    }
                } else {
                    if (gridView.getAdapter() != listAdapter) {
                        int top = getCurrentTop();
                        searchEmptyView.setText(LocaleController.getString("NoChats", R.string.NoChats));
                        gridView.setAdapter(listAdapter);
                        listAdapter.notifyDataSetChanged();
                        if (top > 0) {
                            layoutManager.scrollToPositionWithOffset(0, -top);
                        }
                    }
                }
                if (searchAdapter != null) {
                    searchAdapter.searchDialogs(text);
                }
            }
        });

        gridView = new RecyclerListView(context);
        gridView.setTag(13);
        gridView.setPadding(0, 0, 0, AndroidUtilities.dp(8));
        gridView.setClipToPadding(false);
        gridView.setLayoutManager(layoutManager = new GridLayoutManager(getContext(), 4));
        gridView.setHorizontalScrollBarEnabled(false);
        gridView.setVerticalScrollBarEnabled(false);
        gridView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(android.graphics.Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                RecyclerListView.Holder holder = (RecyclerListView.Holder) parent.getChildViewHolder(view);
                if (holder != null) {
                    int pos = holder.getAdapterPosition();
                    outRect.left = pos % 4 == 0 ? 0 : AndroidUtilities.dp(4);
                    outRect.right = pos % 4 == 3 ? 0 : AndroidUtilities.dp(4);
                } else {
                    outRect.left = AndroidUtilities.dp(4);
                    outRect.right = AndroidUtilities.dp(4);
                }
            }
        });
        containerView.addView(gridView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 88, 0, 0));
        gridView.setAdapter(listAdapter = new ShareDialogsAdapter(context));
        gridView.setGlowColor(Theme.getColor(Theme.key_dialogScrollGlow));
        gridView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (position < 0) {
                    return;
                }
                TLRPC.TL_dialog dialog;
                if (gridView.getAdapter() == listAdapter) {
                    dialog = listAdapter.getItem(position);
                } else {
                    dialog = searchAdapter.getItem(position);
                }
                if (dialog == null) {
                    return;
                }
                ShareDialogCell cell = (ShareDialogCell) view;
                if (selectedDialogs.containsKey(dialog.id)) {
                    selectedDialogs.remove(dialog.id);
                    cell.setChecked(false, true);
                } else {
                    selectedDialogs.put(dialog.id, dialog);
                    cell.setChecked(true, true);
                }
                updateSelectedCount();
            }
        });
        gridView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateLayout();
            }
        });

        onTouchListener = new DialogsOnTouch(context);
        gridView.setOnTouchListener(onTouchListener);

        searchEmptyView = new EmptyTextProgressView(context);
        searchEmptyView.setShowAtCenter(true);
        searchEmptyView.showTextView();
        searchEmptyView.setText(LocaleController.getString("NoChats", R.string.NoChats));
        gridView.setEmptyView(searchEmptyView);
        containerView.addView(searchEmptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 88, 0, 0));


        containerView.addView(frameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.TOP));

        shadow = new View(context);
        shadow.setBackgroundResource(R.drawable.header_shadow);
        containerView.addView(shadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3, Gravity.TOP | Gravity.LEFT, 0, 88, 0, 0));

        frameLayout2 = new FrameLayout(context);
        frameLayout2.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        frameLayout2.setTranslationY(AndroidUtilities.dp(53));
        containerView.addView(frameLayout2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.BOTTOM));
        frameLayout2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        commentTextView = new EditTextBoldCursor(context);
        commentTextView.setHint(LocaleController.getString("ShareComment", R.string.ShareComment));
        commentTextView.setMaxLines(1);
        commentTextView.setSingleLine(true);
        commentTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        commentTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        commentTextView.setBackgroundDrawable(null);
        commentTextView.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        commentTextView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        commentTextView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        commentTextView.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        commentTextView.setCursorSize(AndroidUtilities.dp(20));
        commentTextView.setCursorWidth(1.5f);
        commentTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        frameLayout2.addView(commentTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 8, 1, 8, 0));

        shadow2 = new View(context);
        shadow2.setBackgroundResource(R.drawable.header_shadow_reverse);
        shadow2.setTranslationY(AndroidUtilities.dp(53));
        containerView.addView(shadow2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 3, Gravity.BOTTOM | Gravity.LEFT, 0, 0, 0, 48));


        tabsView = new FrameLayout(context);
        tabsView.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        createTabs(context);
        containerView.addView(tabsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, tabsHeight, Gravity.TOP | Gravity.LEFT, 0, 48, 0, 0));


        tabsView.setOnTouchListener(onTouchListener);
        searchEmptyView.setOnTouchListener(onTouchListener);
        gridView.setMinimumHeight(200);
        searchEmptyView.setMinimumHeight(200);

        updateSelectedCount();

        if (!DialogsActivity.dialogsLoaded[currentAccount]) {
            MessagesController.getInstance(currentAccount).loadDialogs(0, 100, true);
            ContactsController.getInstance(currentAccount).checkInviteText();
            DialogsActivity.dialogsLoaded[currentAccount] = true;
        }
        if (dialogs.isEmpty()) {
            NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.dialogsNeedReload);
        }
    }

    private int getCurrentTop() {
        if (gridView.getChildCount() != 0) {
            View child = gridView.getChildAt(0);
            RecyclerListView.Holder holder = (RecyclerListView.Holder) gridView.findContainingViewHolder(child);
            if (holder != null) {
                return gridView.getPaddingTop() - (holder.getAdapterPosition() == 0 && child.getTop() >= 0 ? child.getTop() : 0);
            }
        }
        return -1000;
    }


    @Override
    public void didReceivedNotification(int id,int account, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload) {
            if (listAdapter != null) {
                listAdapter.fetchDialogs();
            }
            NotificationCenter.getInstance(account).removeObserver(this, NotificationCenter.dialogsNeedReload);
        }
    }

    @Override
    protected boolean canDismissWithSwipe() {
        return false;
    }

    @SuppressLint("NewApi")
    private void updateLayout() {
        if (gridView.getChildCount() <= 0) {
            return;
        }
        View child = gridView.getChildAt(0);
        RecyclerListView.Holder holder = (RecyclerListView.Holder) gridView.findContainingViewHolder(child);
        int top = child.getTop() - AndroidUtilities.dp(8);
        int newOffset = top > 0 && holder != null && holder.getAdapterPosition() == 0 ? top : 0;
        if (scrollOffsetY != newOffset) {
            gridView.setTopGlowOffset(scrollOffsetY = newOffset);
            frameLayout.setTranslationY(scrollOffsetY);
            shadow.setTranslationY(scrollOffsetY);
            tabsView.setTranslationY(scrollOffsetY);
            searchEmptyView.setTranslationY(scrollOffsetY);
            containerView.invalidate();
        }
    }

    private void copyLink(Context context) {
        if (exportedMessageLink == null && linkToCopy == null) {
            return;
        }
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("label", linkToCopy != null ? linkToCopy : exportedMessageLink.link);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, LocaleController.getString("LinkCopied", R.string.LinkCopied), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void showCommentTextView(final boolean show) {
        if (show == (frameLayout2.getTag() != null)) {
            return;
        }
        if (animatorSet != null) {
            animatorSet.cancel();
        }
        frameLayout2.setTag(show ? 1 : null);
        AndroidUtilities.hideKeyboard(commentTextView);
        animatorSet = new AnimatorSet();
        animatorSet.playTogether(
                ObjectAnimator.ofFloat(shadow2, "translationY", AndroidUtilities.dp(show ? 0 : 53)),
                ObjectAnimator.ofFloat(frameLayout2, "translationY", AndroidUtilities.dp(show ? 0 : 53)));
        animatorSet.setInterpolator(new DecelerateInterpolator());
        animatorSet.setDuration(180);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (animation.equals(animatorSet)) {
                    gridView.setPadding(0, 0, 0, AndroidUtilities.dp(show ? 56 : 8));
                    animatorSet = null;
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (animation.equals(animatorSet)) {
                    animatorSet = null;
                }
            }
        });
        animatorSet.start();
    }

    public void updateSelectedCount() {
        if (selectedDialogs.isEmpty()) {
            showCommentTextView(false);
            doneButtonBadgeTextView.setVisibility(View.GONE);
            if (!isPublicChannel && linkToCopy == null) {
                doneButtonTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray4));
                doneButton.setEnabled(false);
                doneButtonTextView.setText(LocaleController.getString("Send", R.string.Send).toUpperCase());
            } else {
                doneButtonTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
                doneButton.setEnabled(true);
                doneButtonTextView.setText(LocaleController.getString("CopyLink", R.string.CopyLink).toUpperCase());
            }
        } else {
            showCommentTextView(true);
            doneButtonTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            doneButtonBadgeTextView.setVisibility(View.VISIBLE);
            doneButtonBadgeTextView.setText(String.format("%d", selectedDialogs.size()));
            doneButtonTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue3));
            doneButton.setEnabled(true);
            doneButtonTextView.setText(LocaleController.getString("Send", R.string.Send).toUpperCase());
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.dialogsNeedReload);
    }

    private ArrayList<TLRPC.TL_dialog> dialogs = new ArrayList<>();

    private class ShareDialogsAdapter extends RecyclerListView.SelectionAdapter {

        private Context context;
        private int currentCount;

        public ShareDialogsAdapter(Context context) {
            this.context = context;
            fetchDialogs();
        }

        public void fetchDialogs() {
            dialogs.clear();
            //Tele X
            if (favsFirst && dialogsType == 0) {
                for (int a = 0; a < MessagesController.getInstance(currentAccount).dialogsFavs.size(); a++) {
                    TLRPC.TL_dialog dialog = MessagesController.getInstance(currentAccount).dialogsFavs.get(a);
                    int lower_id = (int) dialog.id;
                    int high_id = (int) (dialog.id >> 32);
                    if (lower_id != 0 && high_id != 1) {
                        if (lower_id > 0) {
                            dialogs.add(dialog);
                        } else {
                            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-lower_id);
                            if (!(chat == null || ChatObject.isNotInChat(chat) || ChatObject.isChannel(chat) && !chat.creator && (chat.admin_rights == null || !chat.admin_rights.post_messages) && !chat.megagroup)) {
                                dialogs.add(dialog);
                            }
                        }
                    }
                }
            }


            ArrayList<TLRPC.TL_dialog> mdialogs = getDialogsArray();
            for (int a = 0; a < mdialogs.size(); a++) {
                TLRPC.TL_dialog dialog = mdialogs.get(a);
                //Tele X
                /*if (favsFirst && dialogsType == 0 && Favourite.isFavorite(dialog.id)) {
                    continue;
                }*/
                //
                int lower_id = (int) dialog.id;
                int high_id = (int) (dialog.id >> 32);
                if (lower_id != 0 && high_id != 1) {
                    if (lower_id > 0) {
                        dialogs.add(dialog);
                    } else {
                        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-lower_id);
                        if (!(chat == null || ChatObject.isNotInChat(chat) || ChatObject.isChannel(chat) && !chat.creator && (chat.admin_rights == null || !chat.admin_rights.post_messages) && !chat.megagroup)) {
                            dialogs.add(dialog);
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return dialogs.size();
        }

        public TLRPC.TL_dialog getItem(int i) {
            if (i < 0 || i >= dialogs.size()) {
                return null;
            }
            return dialogs.get(i);
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = new ShareDialogCell(context);
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(100)));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ShareDialogCell cell = (ShareDialogCell) holder.itemView;
            TLRPC.TL_dialog dialog = getItem(position);
            cell.setDialog((int) dialog.id, selectedDialogs.containsKey(dialog.id), null);
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }
    }

    public class ShareSearchAdapter extends RecyclerListView.SelectionAdapter {

        private Context context;
        private Timer searchTimer;
        private ArrayList<DialogSearchResult> searchResult = new ArrayList<>();
        private String lastSearchText;
        private int reqId = 0;
        private int lastReqId;
        private int lastSearchId = 0;

        private class DialogSearchResult {
            public TLRPC.TL_dialog dialog = new TLRPC.TL_dialog();
            public TLObject object;
            public int date;
            public CharSequence name;
        }

        public ShareSearchAdapter(Context context) {
            this.context = context;
        }

        private void searchDialogsInternal(final String query, final int searchId) {
            MessagesStorage.getInstance(currentAccount).getStorageQueue().postRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        String search1 = query.trim().toLowerCase();
                        if (search1.length() == 0) {
                            lastSearchId = -1;
                            updateSearchResults(new ArrayList<DialogSearchResult>(), lastSearchId);
                            return;
                        }
                        String search2 = LocaleController.getInstance().getTranslitString(search1);
                        if (search1.equals(search2) || search2.length() == 0) {
                            search2 = null;
                        }
                        String search[] = new String[1 + (search2 != null ? 1 : 0)];
                        search[0] = search1;
                        if (search2 != null) {
                            search[1] = search2;
                        }

                        ArrayList<Integer> usersToLoad = new ArrayList<>();
                        ArrayList<Integer> chatsToLoad = new ArrayList<>();
                        int resultCount = 0;

                        HashMap<Long, DialogSearchResult> dialogsResult = new HashMap<>();
                        SQLiteCursor cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized("SELECT did, date FROM dialogs ORDER BY date DESC LIMIT 400");
                        while (cursor.next()) {
                            long id = cursor.longValue(0);
                            DialogSearchResult dialogSearchResult = new DialogSearchResult();
                            dialogSearchResult.date = cursor.intValue(1);
                            dialogsResult.put(id, dialogSearchResult);

                            int lower_id = (int) id;
                            int high_id = (int) (id >> 32);
                            if (lower_id != 0 && high_id != 1) {
                                if (lower_id > 0) {
                                    if (!usersToLoad.contains(lower_id)) {
                                        usersToLoad.add(lower_id);
                                    }
                                } else {
                                    if (!chatsToLoad.contains(-lower_id)) {
                                        chatsToLoad.add(-lower_id);
                                    }
                                }
                            }
                        }
                        cursor.dispose();

                        if (!usersToLoad.isEmpty()) {
                            cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized(String.format(Locale.US, "SELECT data, status, name FROM users WHERE uid IN(%s)", TextUtils.join(",", usersToLoad)));
                            while (cursor.next()) {
                                String name = cursor.stringValue(2);
                                String tName = LocaleController.getInstance().getTranslitString(name);
                                if (name.equals(tName)) {
                                    tName = null;
                                }
                                String username = null;
                                int usernamePos = name.lastIndexOf(";;;");
                                if (usernamePos != -1) {
                                    username = name.substring(usernamePos + 3);
                                }
                                int found = 0;
                                for (String q : search) {
                                    if (name.startsWith(q) || name.contains(" " + q) || tName != null && (tName.startsWith(q) || tName.contains(" " + q))) {
                                        found = 1;
                                    } else if (username != null && username.startsWith(q)) {
                                        found = 2;
                                    }
                                    if (found != 0) {
                                        NativeByteBuffer data = cursor.byteBufferValue(0);
                                        if (data != null) {
                                            TLRPC.User user = TLRPC.User.TLdeserialize(data, data.readInt32(false), false);
                                            data.reuse();
                                            DialogSearchResult dialogSearchResult = dialogsResult.get((long) user.id);
                                            if (user.status != null) {
                                                user.status.expires = cursor.intValue(1);
                                            }
                                            if (found == 1) {
                                                dialogSearchResult.name = AndroidUtilities.generateSearchName(user.first_name, user.last_name, q);
                                            } else {
                                                dialogSearchResult.name = AndroidUtilities.generateSearchName("@" + user.username, null, "@" + q);
                                            }
                                            dialogSearchResult.object = user;
                                            dialogSearchResult.dialog.id = user.id;
                                            resultCount++;
                                        }
                                        break;
                                    }
                                }
                            }
                            cursor.dispose();
                        }

                        if (!chatsToLoad.isEmpty()) {
                            cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized(String.format(Locale.US, "SELECT data, name FROM chats WHERE uid IN(%s)", TextUtils.join(",", chatsToLoad)));
                            while (cursor.next()) {
                                String name = cursor.stringValue(1);
                                String tName = LocaleController.getInstance().getTranslitString(name);
                                if (name.equals(tName)) {
                                    tName = null;
                                }
                                for (int a = 0; a < search.length; a++) {
                                    String q = search[a];
                                    if (name.startsWith(q) || name.contains(" " + q) || tName != null && (tName.startsWith(q) || tName.contains(" " + q))) {
                                        NativeByteBuffer data = cursor.byteBufferValue(0);
                                        if (data != null) {
                                            TLRPC.Chat chat = TLRPC.Chat.TLdeserialize(data, data.readInt32(false), false);
                                            data.reuse();
                                            if (!(chat == null || ChatObject.isNotInChat(chat) || ChatObject.isChannel(chat) && !chat.creator && (chat.admin_rights == null || !chat.admin_rights.post_messages) && !chat.megagroup)) {
                                                DialogSearchResult dialogSearchResult = dialogsResult.get(-(long) chat.id);
                                                dialogSearchResult.name = AndroidUtilities.generateSearchName(chat.title, null, q);
                                                dialogSearchResult.object = chat;
                                                dialogSearchResult.dialog.id = -chat.id;
                                                resultCount++;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                            cursor.dispose();
                        }

                        ArrayList<DialogSearchResult> searchResults = new ArrayList<>(resultCount);
                        for (DialogSearchResult dialogSearchResult : dialogsResult.values()) {
                            if (dialogSearchResult.object != null && dialogSearchResult.name != null) {
                                searchResults.add(dialogSearchResult);
                            }
                        }

                        cursor = MessagesStorage.getInstance(currentAccount).getDatabase().queryFinalized("SELECT u.data, u.status, u.name, u.uid FROM users as u INNER JOIN contacts as c ON u.uid = c.uid");
                        while (cursor.next()) {
                            int uid = cursor.intValue(3);
                            if (dialogsResult.containsKey((long) uid)) {
                                continue;
                            }
                            String name = cursor.stringValue(2);
                            String tName = LocaleController.getInstance().getTranslitString(name);
                            if (name.equals(tName)) {
                                tName = null;
                            }
                            String username = null;
                            int usernamePos = name.lastIndexOf(";;;");
                            if (usernamePos != -1) {
                                username = name.substring(usernamePos + 3);
                            }
                            int found = 0;
                            for (String q : search) {
                                if (name.startsWith(q) || name.contains(" " + q) || tName != null && (tName.startsWith(q) || tName.contains(" " + q))) {
                                    found = 1;
                                } else if (username != null && username.startsWith(q)) {
                                    found = 2;
                                }
                                if (found != 0) {
                                    NativeByteBuffer data = cursor.byteBufferValue(0);
                                    if (data != null) {
                                        TLRPC.User user = TLRPC.User.TLdeserialize(data, data.readInt32(false), false);
                                        data.reuse();
                                        DialogSearchResult dialogSearchResult = new DialogSearchResult();
                                        if (user.status != null) {
                                            user.status.expires = cursor.intValue(1);
                                        }
                                        dialogSearchResult.dialog.id = user.id;
                                        dialogSearchResult.object = user;
                                        if (found == 1) {
                                            dialogSearchResult.name = AndroidUtilities.generateSearchName(user.first_name, user.last_name, q);
                                        } else {
                                            dialogSearchResult.name = AndroidUtilities.generateSearchName("@" + user.username, null, "@" + q);
                                        }
                                        searchResults.add(dialogSearchResult);
                                    }
                                    break;
                                }
                            }
                        }
                        cursor.dispose();

                        Collections.sort(searchResults, new Comparator<DialogSearchResult>() {
                            @Override
                            public int compare(DialogSearchResult lhs, DialogSearchResult rhs) {
                                if (lhs.date < rhs.date) {
                                    return 1;
                                } else if (lhs.date > rhs.date) {
                                    return -1;
                                }
                                return 0;
                            }
                        });

                        updateSearchResults(searchResults, searchId);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
            });
        }

        private void updateSearchResults(final ArrayList<DialogSearchResult> result, final int searchId) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    if (searchId != lastSearchId) {
                        return;
                    }
                    for (int a = 0; a < result.size(); a++) {
                        DialogSearchResult obj = result.get(a);
                        if (obj.object instanceof TLRPC.User) {
                            TLRPC.User user = (TLRPC.User) obj.object;
                            MessagesController.getInstance(currentAccount).putUser(user, true);
                        } else if (obj.object instanceof TLRPC.Chat) {
                            TLRPC.Chat chat = (TLRPC.Chat) obj.object;
                            MessagesController.getInstance(currentAccount).putChat(chat, true);
                        }
                    }
                    boolean becomeEmpty = !searchResult.isEmpty() && result.isEmpty();
                    boolean isEmpty = searchResult.isEmpty() && result.isEmpty();
                    if (becomeEmpty) {
                        topBeforeSwitch = getCurrentTop();
                    }
                    searchResult = result;
                    notifyDataSetChanged();
                    if (!isEmpty && !becomeEmpty && topBeforeSwitch > 0) {
                        layoutManager.scrollToPositionWithOffset(0, -topBeforeSwitch);
                        topBeforeSwitch = -1000;
                    }
                }
            });
        }

        public void searchDialogs(final String query) {
            if (query != null && lastSearchText != null && query.equals(lastSearchText)) {
                return;
            }
            lastSearchText = query;
            try {
                if (searchTimer != null) {
                    searchTimer.cancel();
                    searchTimer = null;
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            if (query == null || query.length() == 0) {
                searchResult.clear();
                topBeforeSwitch = getCurrentTop();
                notifyDataSetChanged();
            } else {
                final int searchId = ++lastSearchId;
                searchTimer = new Timer();
                searchTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            cancel();
                            searchTimer.cancel();
                            searchTimer = null;
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                        searchDialogsInternal(query, searchId);
                    }
                }, 200, 300);
            }
        }

        @Override
        public int getItemCount() {
            return searchResult.size();
        }

        public TLRPC.TL_dialog getItem(int i) {
            if (i < 0 || i >= searchResult.size()) {
                return null;
            }
            return searchResult.get(i).dialog;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = new ShareDialogCell(context);
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(100)));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ShareDialogCell cell = (ShareDialogCell) holder.itemView;
            DialogSearchResult result = searchResult.get(position);
            cell.setDialog((int) result.dialog.id, selectedDialogs.containsKey(result.dialog.id), result.name);
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }
    }


    public void setCheck(boolean checked) {
        if (Build.VERSION.SDK_INT < 11) {
            quoteSwitch.resetLayout();
            quoteSwitch.requestLayout();
        }
        quoteSwitch.setChecked(checked);
        setCheckColor();
    }

    private void setCheckColor() {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
       // quoteSwitch.setColor(themePrefs.getInt("chatAttachTextColor", 0xffd94c3a));
    }


    public TextView getDoneButtonTextView() {
        return doneButtonTextView;
    }

    public void DoneClicked() {
        if (selectedDialogs.isEmpty() && isPublicChannel) {
            if (loadingLink) {
                copyLinkOnEnd = true;
                Toast.makeText(ShareAlert.this.getContext(), LocaleController.getString("Loading", R.string.Loading), Toast.LENGTH_SHORT).show();
            } else {
                copyLink(ShareAlert.this.getContext());
            }
            dismiss();
        } else {
            for (HashMap.Entry<Long, TLRPC.TL_dialog> entry : selectedDialogs.entrySet()) {
                TLRPC.TL_dialog dialog = entry.getValue();
                int lower_id = (int) dialog.id;
                if (lower_id < 0) {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-lower_id);
                }
                Log.i("TAG", "DoneClicked: entry.getKey() = "+entry.getKey());
                if (quoteSwitch.isChecked()) {
                    SendMessagesHelper.getInstance(currentAccount).sendMessage(sendingMessageObjects, entry.getKey());
                } else {
                    for (MessageObject object : sendingMessageObjects) {
                        SendMessagesHelper.getInstance(currentAccount).processForwardFromMyName(object, entry.getKey());
                    }
                }
            }
            dismiss();
        }
    }


    private void createTabs(final Context context) {


        tabsLayout = new LinearLayout(context);
        tabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabsLayout.setGravity(Gravity.CENTER);

        //1
        allTab = new ImageView(context);
        //allTab.setScaleType(ImageView.ScaleType.CENTER);

        Drawable tab_all = context.getResources().getDrawable(R.drawable.tab_all);
        tab_all.setColorFilter(AndroidUtilities.getIntDef("chatHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);
        allTab.setImageDrawable(tab_all);
        addTabView(context, allTab);

        Drawable tab_user = context.getResources().getDrawable(R.drawable.tab_user);
        tab_user.setColorFilter(AndroidUtilities.getIntDef("chatHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);

        //2
        usersTab = new ImageView(context);
        usersTab.setImageDrawable(tab_user);
        addTabView(context, usersTab);

        Drawable tab_group = context.getResources().getDrawable(R.drawable.tab_group);
        tab_group.setColorFilter(AndroidUtilities.getIntDef("chatHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);

        groupsTab = new ImageView(context);
        groupsTab.setImageDrawable(tab_group);
        addTabView(context, groupsTab);

        //4
        Drawable tab_supergroup = context.getResources().getDrawable(R.drawable.tab_supergroup);
        tab_supergroup.setColorFilter(AndroidUtilities.getIntDef("chatHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);

        superGroupsTab = new ImageView(context);
        superGroupsTab.setImageDrawable(tab_supergroup);

        addTabView(context, superGroupsTab);

        //5
        Drawable tab_channel = context.getResources().getDrawable(R.drawable.tab_channel);
        tab_channel.setColorFilter(AndroidUtilities.getIntDef("chatHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);

        channelsTab = new ImageView(context);
        channelsTab.setImageDrawable(tab_channel);

        addTabView(context, channelsTab);


        //6
        Drawable tab_bot = context.getResources().getDrawable(R.drawable.tab_bot);
        tab_bot.setColorFilter(AndroidUtilities.getIntDef("chatHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);

        botsTab = new ImageView(context);
        botsTab.setImageDrawable(tab_bot);
        addTabView(context, botsTab);

        //7
        Drawable tab_favs = context.getResources().getDrawable(R.drawable.tab_favs);
        tab_favs.setColorFilter(AndroidUtilities.getIntDef("chatHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);

        favsTab = new ImageView(context);
        favsTab.setImageDrawable(tab_favs);

        addTabView(context, favsTab);

        //8
        Drawable tab_contacts = context.getResources().getDrawable(R.drawable.menu_contacts);
        tab_contacts.setColorFilter(AndroidUtilities.getIntDef("chatHeaderIconsColor", 0xffffffff), PorterDuff.Mode.MULTIPLY);

        contactsTab = new ImageView(context);
        contactsTab.setImageDrawable(tab_contacts);

        addTabView(context, contactsTab);

        tabsView.addView(tabsLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        refreshTabs(context);

        allTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 0) {
                    dialogsType = 0;
                    refreshAdapter(context);
                }
            }
        });


        usersTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 3) {
                    dialogsType = 3;
                    refreshAdapter(context);
                }
            }
        });


        groupsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 4) {
                    dialogsType = 4;
                    refreshAdapter(context);
                }
            }
        });

        superGroupsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 7) {
                    dialogsType = 7;
                    refreshAdapter(context);
                }
            }
        });


        channelsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 5) {
                    dialogsType = 5;
                    refreshAdapter(context);
                }
            }
        });


        botsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 6) {
                    dialogsType = 6;
                    refreshAdapter(context);
                }
            }
        });


        favsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 8) {
                    dialogsType = 8;
                    refreshAdapter(context);
                }
            }
        });

        contactsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dialogsType != 15) {
                    dialogsType = 15;
                    refreshAdapter(context);
                }
            }
        });


    }


    private void addTabView(Context context, ImageView iv) {

        iv.setScaleType(ImageView.ScaleType.CENTER);

        RelativeLayout layout = new RelativeLayout(context);
        layout.addView(iv, LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        tabsLayout.addView(layout, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));


    }


    private void refreshAdapter(Context context) {
        if (checked) {
            checkBox.setChecked(false, false);
            checked = false;
        }
        refreshAdapterAndTabs(new ShareDialogsAdapter(context), context);
    }

    private void refreshAdapterAndTabs(ShareDialogsAdapter adapter, Context context) {
        listAdapter = adapter;
        gridView.setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
        refreshTabs(context);
    }

    private void refreshTabs(Context context) {
        //resetTabs();
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int defColor = themePrefs.getInt("chatsHeaderIconsColor", 0xffffffff);
        int iconColor = themePrefs.getInt("chatsHeaderTabIconColor", defColor);

        //int iColor = themePrefs.getInt("chatsHeaderTabUnselectedIconColor", AndroidUtilities.getIntAlphaColor("chatsHeaderTabIconColor", defColor, 0.3f));
        int iColor = themePrefs.getInt("chatsHeaderTabUnselectedIconColor", AndroidUtilities.getIntAlphaColor( defColor, 0.3f));

        allTab.setBackgroundResource(0);
        usersTab.setBackgroundResource(0);
        groupsTab.setBackgroundResource(0);
        superGroupsTab.setBackgroundResource(0);
        channelsTab.setBackgroundResource(0);
        botsTab.setBackgroundResource(0);
        favsTab.setBackgroundResource(0);
        contactsTab.setBackgroundResource(0);

        allTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        usersTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        groupsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        superGroupsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        channelsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        botsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        favsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        contactsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);

        Drawable selected = context.getResources().getDrawable(R.drawable.tab_selected);
        selected.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);

        switch (dialogsType == 9 ? 4 : dialogsType) {
            case 3:
                usersTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                usersTab.setBackgroundDrawable(selected);
                break;
            case 4:
                groupsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                groupsTab.setBackgroundDrawable(selected);
                break;
            case 5:
                channelsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                channelsTab.setBackgroundDrawable(selected);
                break;
            case 6:
                botsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                botsTab.setBackgroundDrawable(selected);
                break;
            case 7:
                superGroupsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                superGroupsTab.setBackgroundDrawable(selected);
                break;
            case 8:
                favsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                favsTab.setBackgroundDrawable(selected);
                break;

            case 15:
                contactsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                contactsTab.setBackgroundDrawable(selected);
                break;

            default:
                allTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                allTab.setBackgroundDrawable(selected);
        }


    }


    private ArrayList<TLRPC.TL_dialog> getDialogsArray() {
        if (dialogsType == 0) {

            sortDefault(MessagesController.getInstance(currentAccount).dialogs);

            return MessagesController.getInstance(currentAccount).dialogs;
        } else if (dialogsType == 1) {
            return MessagesController.getInstance(currentAccount).dialogsServerOnly;
        } else if (dialogsType == 2) {
            return MessagesController.getInstance(currentAccount).dialogsGroupsOnly;
        } else if (dialogsType == 3) {

            return MessagesController.getInstance(currentAccount).dialogsUsers;
        } else if (dialogsType == 4) {

            sortDefault(MessagesController.getInstance(currentAccount).dialogsGroups);

            return MessagesController.getInstance(currentAccount).dialogsGroups;
        } else if (dialogsType == 5) {

            sortDefault(MessagesController.getInstance(currentAccount).dialogsChannels);

            return MessagesController.getInstance(currentAccount).dialogsChannels;
        } else if (dialogsType == 6) {

            sortDefault(MessagesController.getInstance(currentAccount).dialogsBots);

            return MessagesController.getInstance(currentAccount).dialogsBots;
        } else if (dialogsType == 7) {

            sortDefault(MessagesController.getInstance(currentAccount).dialogsMegaGroups);

            return MessagesController.getInstance(currentAccount).dialogsMegaGroups;
        } else if (dialogsType == 8) {

            sortDefault(MessagesController.getInstance(currentAccount).dialogsFavs);

            return MessagesController.getInstance(currentAccount).dialogsFavs;
        } else if (dialogsType == 9) {

            sortDefault(MessagesController.getInstance(currentAccount).dialogsGroupsAll);

            return MessagesController.getInstance(currentAccount).dialogsGroupsAll;
        } else if (dialogsType == 15) {
            ArrayList<TLRPC.TL_dialog> arrayList = new ArrayList();
            Iterator it = ContactsController.getInstance(currentAccount).contacts.iterator();
            while (it.hasNext()) {
                TLRPC.TL_contact tL_contact = (TLRPC.TL_contact) it.next();
                TLRPC.TL_dialog tL_dialog = new TLRPC.TL_dialog();
                tL_dialog.id = (long) tL_contact.user_id;
                arrayList.add(tL_dialog);
            }
            return arrayList;
        }
        return null;
    }


    private void sortDefault(ArrayList<TLRPC.TL_dialog> dialogs) {
        Collections.sort(dialogs, new Comparator<TLRPC.TL_dialog>() {
            @Override
            public int compare(TLRPC.TL_dialog dialog, TLRPC.TL_dialog dialog2) {

                if (dialog.last_message_date == dialog2.last_message_date) {
                    return 0;
                } else if (dialog.last_message_date < dialog2.last_message_date) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });
    }


    public Switch getQuoteSwitch() {
        return quoteSwitch;
    }


    private void setCheckAll(boolean check) {

        for (int i = 0; i < dialogs.size(); i++) {
            TLRPC.TL_dialog dialog = dialogs.get(i);

            if (!check) {
                selectedDialogs.clear();
//                cell.setChecked(false, true);
            } else if (check) {
                selectedDialogs.put(dialog.id, dialog);
//                cell.setChecked(true, true);
            }
            updateSelectedCount();

        }


        listAdapter.notifyDataSetChanged();

    }


    // telex onTouch

    public class DialogsOnTouch implements View.OnTouchListener {

        private DisplayMetrics displayMetrics;
        //private static final String logTag = "SwipeDetector";
        private static final int MIN_DISTANCE_HIGH = 40;
        private static final int MIN_DISTANCE_HIGH_Y = 60;
        private float downX, downY, upX, upY;
        private float vDPI;

        Context mContext;

        public DialogsOnTouch(Context context) {
            this.mContext = context;
            displayMetrics = context.getResources().getDisplayMetrics();
            vDPI = displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT;
            //Log.e("DialogsActivity","DialogsOnTouch vDPI " + vDPI);
        }

        public boolean onTouch(View view, MotionEvent event) {

            touchPositionDP = Math.round(event.getX() / vDPI);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    downX = Math.round(event.getX() / vDPI);
                    downY = Math.round(event.getY() / vDPI);
                    return view instanceof LinearLayout; // for emptyView
                }
                case MotionEvent.ACTION_UP: {
                    upX = Math.round(event.getX() / vDPI);
                    upY = Math.round(event.getY() / vDPI);
                    float deltaX = downX - upX;
                    float deltaY = downY - upY;
                    if (Math.abs(deltaX) > MIN_DISTANCE_HIGH && Math.abs(deltaY) < MIN_DISTANCE_HIGH_Y) {
                        refreshDialogType(deltaX < 0 ? 0 : 1);//0: Left - Right 1: Right - Left
                        downX = Math.round(event.getX() / vDPI);
                        refreshAdapter(mContext);
                    }
                    return false;
                }
            }

            return false;
        }
    }


    private void refreshDialogType(int d) {
        if (d == 1) {
            switch (dialogsType) {
                case 3: // Users
                    dialogsType = 4;
                    break;
                case 4: //Groups
                    dialogsType = 7;
                    break;
                case 7: //Supergroups
                    dialogsType = 5;
                    break;
                case 5: //Channels
                    dialogsType = 6;
                    break;
                case 6: //Bots
                    dialogsType = 8;
                    break;
                case 8: //Favorites
                    dialogsType = 15;
                    break;
                case 15: //contacts
                    dialogsType = 0;
                    break;
                default: //All
                    dialogsType = 3;
            }
        } else {
            switch (dialogsType) {
                case 3: // Users
                    dialogsType = 0;
                    break;
                case 4: //Groups
                    dialogsType = 3;
                    break;
                case 7: //Supergroups
                    dialogsType = 4;
                    break;
                case 5: //Channels
                    dialogsType = 7;
                    break;
                case 6: //Bots
                    dialogsType = 5;
                    break;
                case 8: //Favorites
                    dialogsType = 6;
                    break;
                case 15: //contacts
                    dialogsType = 8;
                    break;
                default: //All
                    dialogsType = 15;
            }
        }


        refreshAdapter(getContext());
    }


}
