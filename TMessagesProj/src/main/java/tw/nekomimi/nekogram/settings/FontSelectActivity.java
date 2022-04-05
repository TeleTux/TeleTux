package tw.nekomimi.nekogram.settings;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;

public class FontSelectActivity extends BaseFragment {

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
    }

    @Override
    public View createView(final Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("FontChange", R.string.FontChange));
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
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        //scrollView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        scrollView.addView(linearLayout);
        CreateFontRow(linearLayout, LocaleController.getString("DefaultFont" ,R.string.DefaultFont), "Vazirmatn-Regular.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("VazirmatnLight" ,R.string.IranSansLight), "Vazirmatn-Light.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("VazirmatnBold" ,R.string.IranSans), "Vazirmatn-Bold.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("VazirmatnBlack" ,R.string.IranSansMedium), "Vazirmatn-Black.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("VazirmatnExtraBold" ,R.string.IranSansBold), "Vazirmatn-ExtraBold.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("VazirmatnExtraLight" ,R.string.Yekan), "Vazirmatn-ExtraLight.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("VazirmatnMedium" ,R.string.Homa), "Vazirmatn-Medium.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("VazirmatnSemiBold" ,R.string.Handwrite), "Vazirmatn-SemiBold.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("VazirmatnThin" ,R.string.Morvarid), "Vazirmatn-Thin.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("Samim" ,R.string.Badkhat), "Samim.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("SamimMedium" ,R.string.Badkhat), "Samim-Medium.ttf", true);
        CreateFontRow(linearLayout, LocaleController.getString("SamimBold" ,R.string.Badkhat), "Samim-Bold.ttf", true);
        frameLayout.addView(scrollView);
        return fragmentView;
    }

    void CreateFontRow(LinearLayout linearLayout, String fontName, final String fontPath, boolean divider) {
        TextSettingsCell cell = new TextSettingsCell(getParentActivity());
        cell.setText(fontName, divider);
        cell.setFont("fonts/" + fontPath);
        cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        TypedValue outValue = new TypedValue();
        getParentActivity().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        cell.setBackgroundResource(outValue.resourceId);

        cell.setOnClickListener(view -> {
            ApplicationLoader.TuxPreferences.edit().putString("Font", fontPath).commit();
            Intent mStartActivity = new Intent(getParentActivity(), LaunchActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent = PendingIntent.getActivity(getParentActivity(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) getParentActivity().getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        });
        linearLayout.addView(cell);
    }
}