/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package tw.nekomimi.nekogram.proxy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.v2ray.ang.V2RayConfig;
import com.v2ray.ang.dto.AngConfig;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

import cn.hutool.core.util.StrUtil;
import kotlin.Unit;
import tw.nekomimi.nekogram.ui.PopupBuilder;

public class VmessSettingsActivity extends BaseFragment {

    private EditTextBoldCursor[] inputFields;

    private EditTextBoldCursor ipField;
    private EditTextBoldCursor portField;
    private EditTextBoldCursor userIdField;
    private EditTextBoldCursor alterIdField;
    private TextSettingsCell securityField;
    private TextSettingsCell networkField;
    private TextSettingsCell headTypeField;
    private EditTextBoldCursor requestHostField;
    private EditTextBoldCursor pathField;
    private TextCheckCell useTlsField;
    private EditTextBoldCursor remarksField;

    private ScrollView scrollView;
    private LinearLayout linearLayout2;
    private LinearLayout inputFieldsContainer;

    private TextInfoPrivacyCell bottomCell;
    private ActionBarMenuItem doneItem;

    private SharedConfig.VmessProxy currentProxyInfo;
    private AngConfig.VmessBean currentBean;

    private boolean ignoreOnTextChange;

    private static final int done_button = 1;

    private static String[] securitySet = { "chacha20-poly1305","aes-128-gcm","auto","none","zero" };
    private static String[] networkSet = { "tcp","kcp","ws","h2","quic" };
    private static String[] headTypeSet = { "none","http","srtp","utp","wechat-video","dtls","wireguard" };

    public class TypeCell extends FrameLayout {

        private TextView textView;
        private ImageView checkImage;
        private boolean needDivider;

        public TypeCell(Context context) {
            super(context);

            setWillNotDraw(false);

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, LocaleController.isRTL ? 23 + 48 : 21, 0, LocaleController.isRTL ? 21 : 23, 0));

            checkImage = new ImageView(context);
            checkImage.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addedIcon), PorterDuff.Mode.SRC_IN));
            checkImage.setImageResource(R.drawable.sticker_added);
            addView(checkImage, LayoutHelper.createFrame(19, 14, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 21, 0, 21, 0));

        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
        }

        public void setValue(String name, boolean checked, boolean divider) {
            textView.setText(name);
            checkImage.setVisibility(checked ? VISIBLE : INVISIBLE);
            needDivider = divider;
        }

        public void setTypeChecked(boolean value) {
            checkImage.setVisibility(value ? VISIBLE : INVISIBLE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (needDivider) {
                canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }

    public VmessSettingsActivity() {
        super();
        currentBean = new AngConfig.VmessBean();
        currentBean.setConfigType(V2RayConfig.EConfigType.Vmess);
    }

    public VmessSettingsActivity(SharedConfig.VmessProxy proxyInfo) {
        super();
        currentProxyInfo = proxyInfo;
        currentBean = proxyInfo.bean;
    }


    @Override
    public void onResume() {
        super.onResume();
        AndroidUtilities.requestAdjustResize(getParentActivity(), classGuid);
    }

    @Override
    public View createView(Context context) {
        actionBar.setTitle(LocaleController.getString("ProxyDetails", R.string.ProxyDetails));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(false);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {

                    if (getParentActivity() == null) {
                        return;
                    }

                    if (StrUtil.isBlank(ipField.getText())) {

                        ipField.requestFocus();
                        AndroidUtilities.showKeyboard(ipField);

                        return;

                    }

                    if (StrUtil.isBlank(portField.getText())) {

                        portField.requestFocus();
                        AndroidUtilities.showKeyboard(portField);

                        return;

                    }

                    if (StrUtil.isBlank(userIdField.getText())) {

                        userIdField.requestFocus();
                        AndroidUtilities.showKeyboard(userIdField);

                        return;

                    }

                    if (StrUtil.isBlank(alterIdField.getText())) {

                        alterIdField.requestFocus();
                        AndroidUtilities.showKeyboard(alterIdField);

                        return;

                    }

                    currentBean.setAddress(ipField.getText().toString());
                    currentBean.setPort(Utilities.parseInt(portField.getText().toString()));
                    currentBean.setId(userIdField.getText().toString());
                    currentBean.setAlterId(Utilities.parseInt(alterIdField.getText().toString()));
                    currentBean.setSecurity(securityField.getValueTextView().getText().toString());
                    currentBean.setNetwork(networkField.getValueTextView().getText().toString());
                    currentBean.setHeaderType(headTypeField.getValueTextView().getText().toString());
                    currentBean.setRequestHost(requestHostField.getText().toString());
                    currentBean.setPath(pathField.getText().toString());
                    currentBean.setStreamSecurity(useTlsField.isChecked() ? "tls" : "");
                    currentBean.setRemarks(remarksField.getText().toString());

                    if (currentProxyInfo == null) {
                        currentProxyInfo = new SharedConfig.VmessProxy(currentBean);
                        SharedConfig.addProxy(currentProxyInfo);
                        SharedConfig.setCurrentProxy(currentProxyInfo);
                    } else {
                        currentProxyInfo.proxyCheckPingId = 0;
                        currentProxyInfo.availableCheckTime = 0;
                        currentProxyInfo.ping = 0;
                        SharedConfig.saveProxyList();
                        SharedConfig.setProxyEnable(false);
                    }

                    finishFragment();

                }
            }
        });

        doneItem = actionBar.createMenu().addItemWithWidth(done_button, R.drawable.ic_done, AndroidUtilities.dp(56));
        doneItem.setContentDescription(LocaleController.getString("Done", R.string.Done));

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        AndroidUtilities.setScrollViewEdgeEffectColor(scrollView, Theme.getColor(Theme.key_actionBarDefault));
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(linearLayout2, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        inputFieldsContainer = new LinearLayout(context);
        inputFieldsContainer.setOrientation(LinearLayout.VERTICAL);
        inputFieldsContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // bring to front for transitions
            inputFieldsContainer.setElevation(AndroidUtilities.dp(1f));
            inputFieldsContainer.setOutlineProvider(null);
        }
        linearLayout2.addView(inputFieldsContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        inputFields =  new EditTextBoldCursor[7];

        for (int a = 0; a < 7; a++) {
            FrameLayout container = new FrameLayout(context);
            EditTextBoldCursor cursor = mkCursor();
            inputFields[a] = cursor;
            cursor.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
            switch (a) {
                case 0:
                    ipField = cursor;
                    cursor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                    cursor.setHintText(LocaleController.getString("UseProxyAddress", R.string.UseProxyAddress));
                    cursor.setText(currentBean.getAddress());
                    break;
                case 1:
                    portField = cursor;
                    cursor.setInputType(InputType.TYPE_CLASS_NUMBER);
                    cursor.setHintText(LocaleController.getString("UseProxyPort", R.string.UseProxyPort));
                    cursor.setText("" + currentBean.getPort());
                    break;
                case 2:
                    userIdField = cursor;
                    cursor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                    cursor.setHintText(LocaleController.getString("VmessUserId", R.string.VmessUserId));
                    cursor.setText(currentBean.getId());
                    break;
                case 3:
                    alterIdField = cursor;
                    cursor.setInputType(InputType.TYPE_CLASS_NUMBER);
                    cursor.setHintText(LocaleController.getString("VmessAlterId", R.string.VmessAlterId));
                    cursor.setText("" + currentBean.getAlterId());
                    break;
                case 4:
                    requestHostField = cursor;
                    cursor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                    cursor.setHintText(LocaleController.getString("VmessRequestHost", R.string.VmessRequestHost));
                    cursor.setText(currentBean.getRequestHost());
                    break;
                case 5:
                    pathField = cursor;
                    cursor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                    cursor.setHintText(LocaleController.getString("VmessPath", R.string.VmessPath));
                    cursor.setText(currentBean.getPath());
                    break;
                case 6:
                    remarksField = cursor;
                    cursor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                    cursor.setHintText(LocaleController.getString("ProxyRemarks", R.string.ProxyRemarks));
                    cursor.setText(currentBean.getRemarks());
                    break;
            }
            cursor.setSelection(cursor.length());

            cursor.setPadding(0, 0, 0, 0);
            container.addView(cursor, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 17, a == 0 ? 12 : 0, 17, 0));

        }

        inputFieldsContainer.addView((View) ipField.getParent(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));
        inputFieldsContainer.addView((View) portField.getParent(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));
        inputFieldsContainer.addView((View) userIdField.getParent(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));
        inputFieldsContainer.addView((View) alterIdField.getParent(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

        FrameLayout container = new FrameLayout(context);

        securityField = new TextSettingsCell(context);
        securityField.setBackground(Theme.getSelectorDrawable(false));
        securityField.setTextAndValue(LocaleController.getString("VmessSecurity", R.string.VmessSecurity), currentBean.getSecurity(), false);
        container.addView(securityField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 0, 0, 0, 0));

        securityField.setOnClickListener((v) -> {

            PopupBuilder select = new PopupBuilder(v);

            select.setItems(securitySet, (__,value) -> {

                securityField.getValueTextView().setText(value);

                return Unit.INSTANCE;

            });

            select.show();

        });

        inputFieldsContainer.addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

        container = new FrameLayout(context);
        networkField = new TextSettingsCell(context);
        networkField.setBackground(Theme.getSelectorDrawable(false));
        networkField.setTextAndValue(LocaleController.getString("VmessNetwork", R.string.VmessNetwork), currentBean.getNetwork(), false);
        container.addView(networkField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 0, 0, 0, 0));

        networkField.setOnClickListener((v) -> {

            PopupBuilder select = new PopupBuilder(v);

            select.setItems(networkSet, (__,value) -> {

                networkField.getValueTextView().setText(value);

                return Unit.INSTANCE;

            });

            select.show();

        });

        inputFieldsContainer.addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

        container = new FrameLayout(context);
        headTypeField = new TextSettingsCell(context);
        headTypeField.setBackground(Theme.getSelectorDrawable(false));
        headTypeField.setTextAndValue(LocaleController.getString("VmessHeadType", R.string.VmessHeadType), currentBean.getHeaderType(), false);
        container.addView(headTypeField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 0, 0, 0, 0));

        headTypeField.setOnClickListener((v) -> {

            PopupBuilder select = new PopupBuilder(v);

            select.setItems(headTypeSet, (__,value) -> {

                headTypeField.getValueTextView().setText(value);

                return Unit.INSTANCE;

            });

            select.show();

        });

        inputFieldsContainer.addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

        inputFieldsContainer.addView((View) requestHostField.getParent(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));
        inputFieldsContainer.addView((View) pathField.getParent(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

        container = new FrameLayout(context);
        useTlsField = new TextCheckCell(context);
        useTlsField.setBackground(Theme.getSelectorDrawable(false));
        useTlsField.setTextAndCheck(LocaleController.getString("VmessTls", R.string.VmessTls), !StrUtil.isBlank(currentBean.getStreamSecurity()), false);
        container.addView(useTlsField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 0, 0, 0, 0));

        useTlsField.setOnClickListener((v) -> useTlsField.setChecked(!useTlsField.isChecked()));

        inputFieldsContainer.addView(container, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

        inputFieldsContainer.addView((View) remarksField.getParent(), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 64));

        bottomCell = new TextInfoPrivacyCell(context);
        bottomCell.setBackground(Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        bottomCell.setText(LocaleController.getString("ProxyInfoVmess", R.string.ProxyInfoVmess));
        linearLayout2.addView(bottomCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return fragmentView;

    }

    EditTextBoldCursor mkCursor() {

        EditTextBoldCursor cursor = new EditTextBoldCursor(getParentActivity());
        cursor.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        cursor.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        cursor.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        cursor.setBackground(null);
        cursor.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        cursor.setCursorSize(AndroidUtilities.dp(20));
        cursor.setCursorWidth(1.5f);
        cursor.setSingleLine(true);
        cursor.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        cursor.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        cursor.setTransformHintToHeader(true);
        cursor.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_windowBackgroundWhiteRedText3));
        return cursor;

    }

    @Override
    protected void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && !backward && currentProxyInfo == null) {
            ipField.requestFocus();
            AndroidUtilities.showKeyboard(ipField);
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        final ThemeDescription.ThemeDescriptionDelegate delegate = () -> {
            if (inputFields != null) {
                for (int i = 0; i < inputFields.length; i++) {
                    inputFields[i].setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField),
                            Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated),
                            Theme.getColor(Theme.key_windowBackgroundWhiteRedText3));
                }
            }
        };
        ArrayList<ThemeDescription> arrayList = new ArrayList<>();
        arrayList.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        arrayList.add(new ThemeDescription(scrollView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch));
        arrayList.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder));
        arrayList.add(new ThemeDescription(inputFieldsContainer, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        arrayList.add(new ThemeDescription(linearLayout2, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, delegate, Theme.key_windowBackgroundWhiteBlueText4));
        arrayList.add(new ThemeDescription(null, 0, null, null, null, null, delegate, Theme.key_windowBackgroundWhiteGrayText2));


        if (inputFields != null) {
            for (int a = 0; a < inputFields.length; a++) {
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_HINTTEXTCOLOR | ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));
                arrayList.add(new ThemeDescription(inputFields[a], ThemeDescription.FLAG_CURSORCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
                arrayList.add(new ThemeDescription(null, 0, null, null, null, delegate, Theme.key_windowBackgroundWhiteInputField));
                arrayList.add(new ThemeDescription(null, 0, null, null, null, delegate, Theme.key_windowBackgroundWhiteInputFieldActivated));
                arrayList.add(new ThemeDescription(null, 0, null, null, null, delegate, Theme.key_windowBackgroundWhiteRedText3));
            }
        } else {
            arrayList.add(new ThemeDescription(null, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            arrayList.add(new ThemeDescription(null, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
        }

        arrayList.add(new ThemeDescription(bottomCell, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        arrayList.add(new ThemeDescription(bottomCell, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));
        arrayList.add(new ThemeDescription(bottomCell, ThemeDescription.FLAG_LINKCOLOR, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteLinkText));

        return arrayList;
    }
}
