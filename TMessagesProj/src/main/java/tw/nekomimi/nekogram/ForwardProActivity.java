package tw.nekomimi.nekogram;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.googlecode.mp4parser.authoring.tracks.h265.NalUnitTypes;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.ShareAlert;
import java.util.Calendar;

import static com.google.android.exoplayer2.C.ENCODING_PCM_32BIT;


public class ForwardProActivity extends BaseFragment {

    private int currentAccount = UserConfig.selectedAccount;

    private MessageObject selectedObject;
    protected ChatActivityEnterView chatActivityEnterView;
    private FrameLayout emptyViewContainer;
    private TextView media;
    private TextView mediaCaption;
    private final static int id_chat_compose_panel = 1000;
    private String caption = "";
    protected TLRPC.Chat currentChat;
    private Bitmap imaage = null;
    int mode = 0;
    long timer = 0;

    public ForwardProActivity(MessageObject selectedObject, TLRPC.Chat currentChat) {
        this.selectedObject = new MessageObject(currentAccount,newMessage(selectedObject.messageOwner),  true);
        this.selectedObject.photoThumbs = selectedObject.photoThumbs;
        this.currentChat = currentChat;
    }

    public ForwardProActivity(MessageObject selectedObject, TLRPC.Chat currentChat, int i) {
        this.selectedObject = new MessageObject(currentAccount,newMessage(selectedObject.messageOwner), true);
        this.selectedObject.photoThumbs = selectedObject.photoThumbs;
        this.currentChat = currentChat;
        this.mode = i;
    }

    @Override
    public View createView(final Context context) {
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));

        actionBar.setItemsBackgroundColor(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, false);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(mode == 1 ? LocaleController.getString("ProForward", R.string.ProForward) : LocaleController.getString("TimedForward", R.string.TimedForward));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });


        fragmentView = new FrameLayout(context);
        fragmentView.setLayoutParams(new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        MessageObject myObject = selectedObject;
        if (myObject.caption != null)
            caption = myObject.caption.toString();
        else if (myObject.messageText != null)
            caption = myObject.messageText.toString();
        fragmentView = new SizeNotifierFrameLayout(context) {


            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                boolean result = super.drawChild(canvas, child, drawingTime);
                if (child == actionBar) {
                    parentLayout.drawHeaderShadow(canvas, actionBar.getMeasuredHeight());
                }
                return result;
            }

            @SuppressLint("WrongConstant")
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

                int size = MeasureSpec.getSize(widthMeasureSpec);
                int size2 = MeasureSpec.getSize(heightMeasureSpec);
                setMeasuredDimension(size, size2);
                size2 -= getPaddingTop();
                int emojiPadding = getKeyboardHeight() <= AndroidUtilities.dp(20.0f) ? size2 - chatActivityEnterView.getEmojiPadding() : size2;
                int childCount = getChildCount();
                measureChildWithMargins(chatActivityEnterView, widthMeasureSpec, 0, heightMeasureSpec, 0);
                for (int i3 = 0; i3 < childCount; i3++) {
                    View childAt = getChildAt(i3);
                    if (!(childAt == null || childAt.getVisibility() == GONE || childAt == chatActivityEnterView)) {
                        try {
                            if (childAt == this) {
                                childAt.measure(MeasureSpec.makeMeasureSpec(size, ENCODING_PCM_32BIT), MeasureSpec.makeMeasureSpec(emojiPadding, ENCODING_PCM_32BIT));
                            } else if (chatActivityEnterView.isPopupView(childAt)) {
                                childAt.measure(MeasureSpec.makeMeasureSpec(size, ENCODING_PCM_32BIT), MeasureSpec.makeMeasureSpec(childAt.getLayoutParams().height, ENCODING_PCM_32BIT));
                            } else {
                                measureChildWithMargins(childAt, widthMeasureSpec, 0, heightMeasureSpec, 0);
                            }
                        } catch (Throwable e) {
                            FileLog.e(e);
                        }
                    }
                }
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                int childCount = getChildCount();
                int emojiPadding = getKeyboardHeight() <= AndroidUtilities.dp(20.0f) ? chatActivityEnterView.getEmojiPadding() : 0;
                setBottomClip(emojiPadding);
                for (int i5 = 0; i5 < childCount; i5++) {
                    View childAt = getChildAt(i5);
                    if (childAt.getVisibility() != GONE) {
                        int i6;
                        LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                        int measuredWidth = childAt.getMeasuredWidth();
                        int measuredHeight = childAt.getMeasuredHeight();
                        int i7 = layoutParams.gravity;
                        if (i7 == -1) {
                            i7 = 51;
                        }
                        int i8 = i7 & 112;
                        switch ((i7 & 7) & 7) {
//                            case VideoPlayer.TYPE_AUDIO /*1*/:
//                                i7 = ((((r - l) - measuredWidth) / 2) + layoutParams.leftMargin) - layoutParams.rightMargin;
//                                break;
//                            case VideoPlayer.STATE_ENDED /*5*/:
//                                i7 = (r - measuredWidth) - layoutParams.rightMargin;
//                                break;
                            default:
                                i7 = layoutParams.leftMargin;
                                break;
                        }
                        switch (i8) {
                            case TLRPC.USER_FLAG_PHONE /*16*/:
                                i6 = (((((b - emojiPadding) - t) - measuredHeight) / 2) + layoutParams.topMargin) - layoutParams.bottomMargin;
                                break;
                            case NalUnitTypes.NAL_TYPE_UNSPEC48 /*48*/:
                                i6 = layoutParams.topMargin + getPaddingTop();
                                break;
                            case 80:
                                i6 = (((b - emojiPadding) - t) - measuredHeight) - layoutParams.bottomMargin;
                                break;
                            default:
                                i6 = layoutParams.topMargin;
                                break;
                        }
                        if (chatActivityEnterView.isPopupView(childAt)) {
//                            i6 = chatActivityEnterView.getTop() - childAt.getMeasuredHeight() + AndroidUtilities.dp(1);
//                            i6 = childAt.getMeasuredHeight() - AndroidUtilities.dp(20);
                            i6 = fragmentView.getMeasuredHeight() - chatActivityEnterView.getEmojiView().getMeasuredHeight();

                        }
                        childAt.layout(i7, i6, measuredWidth + i7, measuredHeight + i6);
                    }
                }
                notifyHeightChanged();
            }
        };

        SizeNotifierFrameLayout contentView = (SizeNotifierFrameLayout) fragmentView;
        contentView.setBackgroundImage(Theme.getCachedWallpaper(),false);


        ScrollView scrollView = new ScrollView(context);


        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.TOP);
        l.setPadding(0, 80, 0, 250);

        ChatMessageCell chatMessageCell = new ChatMessageCell(getParentActivity());
        MessageObject temp = selectedObject;


        if (temp.messageOwner != null) {
            temp.messageOwner.message = "";
            if (temp.messageOwner.media != null) {
                // temp.messageOwner.media.caption = "";

            }
        }
        temp.caption = "";
        temp.messageText = "";
        chatMessageCell.setMessageObject(temp, null, false, false);
        chatMessageCell.setOnClickListener(null);
        chatMessageCell.setOnTouchListener(null);
        chatMessageCell.setOnLongClickListener(null);
        chatMessageCell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
            @Override
            public void didPressUserAvatar(ChatMessageCell cell, TLRPC.User user) {

            }

            @Override
            public void didPressViaBot(ChatMessageCell cell, String username) {

            }

            @Override
            public void didPressChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId) {

            }

            @Override
            public void didPressCancelSendButton(ChatMessageCell cell) {

            }

            @Override
            public void didLongPress(ChatMessageCell cell) {

            }

            @Override
            public void didPressReplyMessage(ChatMessageCell cell, int id) {

            }

            @Override
            public void didPressUrl(MessageObject messageObject, CharacterStyle url, boolean longPress) {

            }

            @Override
            public void needOpenWebView(String url, String title, String description, String originalUrl, int w, int h) {

            }

            @Override
            public void didPressImage(ChatMessageCell cell) {

            }

            @Override
            public void didPressShare(ChatMessageCell cell) {

            }

            @Override
            public void didPressOther(ChatMessageCell cell) {

            }

            @Override
            public void didPressBotButton(ChatMessageCell cell, TLRPC.KeyboardButton button) {

            }

            @Override
            public void didPressVoteButton(ChatMessageCell cell, TLRPC.TL_pollAnswer button) {

            }

            @Override
            public void didPressInstantButton(ChatMessageCell cell, int type) {

            }

            @Override
            public boolean isChatAdminCell(int uid) {
                return false;
            }

            @Override
            public boolean needPlayMessage(MessageObject messageObject) {
                return false;
            }

            @Override
            public boolean canPerformActions() {
                return false;
            }

            @Override
            public void videoTimerReached() {

            }
        });
        l.addView(chatMessageCell, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 0, 15));


        emptyViewContainer = new FrameLayout(context);
        emptyViewContainer.setBackgroundColor(-1);
        mediaCaption = new TextView(context);
        mediaCaption.setTextSize(2, 15.f);
        mediaCaption.setPadding(12, 0, 12, 0);
        mediaCaption.setSingleLine(true);
        mediaCaption.setEllipsize(TextUtils.TruncateAt.END);
        mediaCaption.setMaxLines(1);
        mediaCaption.setGravity(Gravity.CENTER_VERTICAL);
        mediaCaption.setBackgroundColor(0xfffafafa);
        mediaCaption.setText(LocaleController.getString("MediaCaption", R.string.MediaCaption) + " : ");
        scrollView.addView(l);
        scrollView.setPadding(0, 80, 0, 250);
        contentView.addView(scrollView, contentView.getChildCount() - 1, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM));


        emptyViewContainer = new FrameLayout(context);
        emptyViewContainer.setVisibility(View.INVISIBLE);
        contentView.addView(emptyViewContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        emptyViewContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });


        chatActivityEnterView = new ChatActivityEnterView(getParentActivity(), contentView, null, false);
        chatActivityEnterView.setDialogId(selectedObject.getDialogId(),currentAccount);

        LinearLayout l2 = new LinearLayout(context);
        l2.setOrientation(LinearLayout.VERTICAL);
        l2.addView(mediaCaption, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 34, Gravity.TOP | (LocaleController.isRTL ? 5 : 3), 0, 0, 0, 0));
        l2.addView(chatActivityEnterView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM));

        contentView.addView(l2, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));


        emptyViewContainer = new FrameLayout(context);
        emptyViewContainer.setBackgroundColor(-1);
        media = new TextView(context);
        media.setTextSize(2, 17.f);
        media.setPadding(12, 0, 12, 0);
        media.setSingleLine(true);
        media.setEllipsize(TextUtils.TruncateAt.END);
        media.setMaxLines(1);
        media.setGravity(Gravity.CENTER_VERTICAL);
        media.setBackgroundColor(0xfffafafa);
        media.setText(LocaleController.getString("Media", R.string.Media) + " : ");
        contentView.addView(media, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 34, Gravity.TOP | (LocaleController.isRTL ? 5 : 3), 0, 0, 0, 0));


        chatActivityEnterView.setDelegate(new ChatActivityEnterView.ChatActivityEnterViewDelegate() {
            @Override
            public void onMessageSend(CharSequence message) {

            }

            @Override
            public void needSendTyping() {

            }

            @Override
            public void onTextChanged(CharSequence text, boolean bigChange) {

            }

            @Override
            public void onTextSelectionChanged(int start, int end) {

            }

            @Override
            public void onTextSpansChanged(CharSequence text) {

            }

            @Override
            public void onAttachButtonHidden() {

            }

            @Override
            public void onAttachButtonShow() {

            }

            @Override
            public void onWindowSizeChanged(int size) {

            }



            @Override
            public void onStickersTab(boolean opened) {

            }

            @Override
            public void onMessageEditEnd(boolean loading) {

            }

            @Override
            public void didPressedAttachButton() {

            }


            @Override
            public void needStartRecordVideo(int state) {

            }

            @Override
            public void needChangeVideoPreviewState(int state, float seekProgress) {

            }

            @Override
            public void onSwitchRecordMode(boolean video) {

            }

            @Override
            public void onPreAudioVideoRecord() {

            }

            @Override
            public void needStartRecordAudio(int state) {

            }

            @Override
            public void needShowMediaBanHint() {

            }

            @Override
            public void onStickersExpandedChange() {

            }


        });
        chatActivityEnterView.setAllowStickersAndGifs(false, false);

        chatActivityEnterView.getMessageEditText().setText(caption);


        if (myObject != null && myObject.messageOwner != null && myObject.messageOwner.media != null) {
            InputFilter[] fa = new InputFilter[1];
            fa[0] = new InputFilter.LengthFilter(200);
            chatActivityEnterView.getMessageEditText().setFilters(fa);
        }


        chatActivityEnterView.getSendButton().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editDone();
            }
        });


        return fragmentView;
    }

    public void setTime(Context context) {

        Calendar mcurrentTime = Calendar.getInstance();
        final Calendar newTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay,
                                          int minute) {
                        Calendar calNow = Calendar.getInstance();
                        Calendar calSet = (Calendar) calNow.clone();

                        calSet.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calSet.set(Calendar.MINUTE, minute);
                        calSet.set(Calendar.SECOND, 0);
                        calSet.set(Calendar.MILLISECOND, 0);


                        if (calSet.compareTo(calNow) <= 0) {
                            calSet.add(Calendar.DATE, 1);
                        }
                        timer = calSet.getTimeInMillis();
                        editDone();
                    }
                }, hour, minute, false);
        timePickerDialog.setTitle(R.string.sendTime);
        timePickerDialog.show();
    }


    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (chatActivityEnterView != null) {
            chatActivityEnterView.onDestroy();
        }
    }


    private void editDone() {


        final MessageObject m = selectedObject;

        if (m != null && chatActivityEnterView.getMessageEditText().getText() != null) {
            m.messageOwner.message = chatActivityEnterView.getMessageEditText().getText().toString();
            if (m.messageOwner.media != null)
                m.messageOwner.media.caption = chatActivityEnterView.getMessageEditText().getText().toString();

            m.caption = chatActivityEnterView.getMessageEditText().getText().toString();
            m.messageText = chatActivityEnterView.getMessageEditText().getText().toString();
            m.messageOwner.from_id = -1;
            m.applyNewText();
        }

        final ShareAlert d = ShareAlert.createShareAlert(getParentActivity(), m, null, ChatObject.isChannel(currentChat) && !currentChat.megagroup && currentChat.username != null && currentChat.username.length() > 0, null, true);
        d.getQuoteSwitch().setChecked(false);
        d.getQuoteSwitch().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (d.getQuoteSwitch().isChecked())
                    d.getQuoteSwitch().setChecked(false);

                Toast.makeText(getParentActivity(), getParentActivity().getResources().getString(R.string.ProForwardError), Toast.LENGTH_LONG).show();

            }
        });
        showDialog(d);
        chatActivityEnterView.openKeyboard();
        d.getDoneButtonTextView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getParentActivity(), LocaleController.getString("Sent", R.string.Sent), Toast.LENGTH_SHORT).show();
                d.DoneClicked();
                ForwardProActivity.this.finishFragment();
            }
        });


    }


    private TLRPC.Message newMessage(TLRPC.Message message) {
        if (message == null) {
            return null;
        }
        TLRPC.Message message2 = new TLRPC.Message();
        if (message instanceof TLRPC.TL_message) {
            message2 = new TLRPC.TL_message();
        } else if (message instanceof TLRPC.TL_message_secret) {
            message2 = new TLRPC.TL_message_secret();
        }
        message2.id = message.id;
        message2.from_id = message.from_id;
        message2.to_id = message.to_id;
        message2.date = message.date;
        message2.action = message.action;
        message2.reply_to_msg_id = message.reply_to_msg_id;
        message2.fwd_from = message.fwd_from;
        message2.reply_to_random_id = message.reply_to_random_id;
        message2.via_bot_name = message.via_bot_name;
        message2.edit_date = message.edit_date;
        message2.silent = message.silent;
        message2.message = message.message;
        if (message.media != null) {
            message2.media = newMessageMedia(message.media);
        }
        message2.flags = message.flags;
        message2.mentioned = message.mentioned;
        message2.media_unread = message.media_unread;
        message2.out = message.out;
        message2.unread = message.unread;
        message2.entities = message.entities;
        message2.reply_markup = message.reply_markup;
        message2.views = message.views;
        message2.via_bot_id = message.via_bot_id;
        message2.send_state = message.send_state;
        message2.fwd_msg_id = message.fwd_msg_id;
        message2.attachPath = message.attachPath;
        message2.params = message.params;
        message2.random_id = message.random_id;
        message2.local_id = message.local_id;
        message2.dialog_id = message.dialog_id;
        message2.ttl = message.ttl;
        message2.destroyTime = message.destroyTime;
        message2.layer = message.layer;
        message2.seq_in = message.seq_in;
        message2.seq_out = message.seq_out;
        message2.replyMessage = message.replyMessage;
        return message2;
    }

    private TLRPC.MessageMedia newMessageMedia(TLRPC.MessageMedia messageMedia) {
        TLRPC.MessageMedia tL_messageMediaUnsupported_old = messageMedia instanceof TLRPC.TL_messageMediaUnsupported_old ? new TLRPC.TL_messageMediaUnsupported_old() : messageMedia instanceof TLRPC.TL_messageMediaAudio_layer45 ? new TLRPC.TL_messageMediaAudio_layer45() : messageMedia instanceof TLRPC.TL_messageMediaPhoto_old ? new TLRPC.TL_messageMediaPhoto_old() : messageMedia instanceof TLRPC.TL_messageMediaUnsupported ? new TLRPC.TL_messageMediaUnsupported() : messageMedia instanceof TLRPC.TL_messageMediaEmpty ? new TLRPC.TL_messageMediaEmpty() : messageMedia instanceof TLRPC.TL_messageMediaVenue ? new TLRPC.TL_messageMediaVenue() : messageMedia instanceof TLRPC.TL_messageMediaVideo_old ? new TLRPC.TL_messageMediaVideo_old() : messageMedia instanceof TLRPC.TL_messageMediaDocument_old ? new TLRPC.TL_messageMediaDocument_old() : messageMedia instanceof TLRPC.TL_messageMediaDocument ? new TLRPC.TL_messageMediaDocument() : messageMedia instanceof TLRPC.TL_messageMediaContact ? new TLRPC.TL_messageMediaContact() : messageMedia instanceof TLRPC.TL_messageMediaPhoto ? new TLRPC.TL_messageMediaPhoto() : messageMedia instanceof TLRPC.TL_messageMediaVideo_layer45 ? new TLRPC.TL_messageMediaVideo_layer45() : messageMedia instanceof TLRPC.TL_messageMediaWebPage ? new TLRPC.TL_messageMediaWebPage() : messageMedia instanceof TLRPC.TL_messageMediaGeo ? new TLRPC.TL_messageMediaGeo() : new TLRPC.MessageMedia();
        tL_messageMediaUnsupported_old.bytes = messageMedia.bytes;
        tL_messageMediaUnsupported_old.caption = messageMedia.caption;
        tL_messageMediaUnsupported_old.photo = messageMedia.photo;
        tL_messageMediaUnsupported_old.audio_unused = messageMedia.audio_unused;
        tL_messageMediaUnsupported_old.geo = messageMedia.geo;
        tL_messageMediaUnsupported_old.title = messageMedia.title;
        tL_messageMediaUnsupported_old.address = messageMedia.address;
        tL_messageMediaUnsupported_old.provider = messageMedia.provider;
        tL_messageMediaUnsupported_old.venue_id = messageMedia.venue_id;
        tL_messageMediaUnsupported_old.document = messageMedia.document;
        tL_messageMediaUnsupported_old.video_unused = messageMedia.video_unused;
        tL_messageMediaUnsupported_old.phone_number = messageMedia.phone_number;
        tL_messageMediaUnsupported_old.first_name = messageMedia.first_name;
        tL_messageMediaUnsupported_old.last_name = messageMedia.last_name;
        tL_messageMediaUnsupported_old.user_id = messageMedia.user_id;
        tL_messageMediaUnsupported_old.webpage = messageMedia.webpage;
        return tL_messageMediaUnsupported_old;
    }


    @Override
    public ThemeDescription[] getThemeDescriptions() {

        return new ThemeDescription[]{

                new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector),
        };
    }


}
