package org.telegram.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MrzRecognizer;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.camera.CameraController;
import org.telegram.messenger.camera.CameraSession;
import org.telegram.messenger.camera.CameraView;
import org.telegram.messenger.camera.Size;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkPath;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.URLSpanNoUnderline;

import java.util.ArrayList;

@TargetApi(18)
public class CameraScanActivity extends BaseFragment {

    private TextView titleTextView;
    private TextView descriptionText;
    private CameraView cameraView;
    private HandlerThread backgroundHandlerThread = new HandlerThread("ScanCamera");
    private Handler handler;
    private TextView recognizedMrzView;
    private Paint paint = new Paint();
    private Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Path path = new Path();
    private ImageView galleryButton;
    private ImageView flashButton;
    private AnimatorSet flashAnimator;
    private float backShadowAlpha = .5f;

    private SpringAnimation qrAppearing = null;
    private float qrAppearingValue = 0;

    private RectF fromBounds = new RectF();
    private RectF bounds = new RectF();
    private long lastBoundsUpdate = 0;
    private final long boundsUpdateDuration = 75;

    private CameraScanActivityDelegate delegate;
    private boolean recognized;
    private long recognizedStart;
    private int recognizeFailed = 0;
    private int recognizeIndex = 0;
    private String recognizedText;

    private int sps; // samples per second (already when recognized)

    private boolean qrLoading = false;
    private boolean qrLoaded = false;

    private QRCodeReader qrReader = null;
    //private BarcodeDetector visionQrReader = null;

    private boolean needGalleryButton;
    private boolean any;

    private int currentType;

    public static final int TYPE_MRZ = 0;
    public static final int TYPE_QR = 1;
    public static final int TYPE_QR_LOGIN = 2;

    public interface CameraScanActivityDelegate {
        default void didFindMrzInfo(MrzRecognizer.Result result) {

        }

        default void didFindQr(String text) {

        }

        default boolean processQr(String text, Runnable onLoadEnd) {
            return false;
        }
    }

    // Official Signature
    public static ActionBarLayout[] showAsSheet(BaseFragment parentFragment, boolean gallery, int type, CameraScanActivityDelegate cameraDelegate) {
        return showAsSheet(parentFragment, gallery, type, cameraDelegate, false);
    }

    // Add the any parameter
    public static ActionBarLayout[] showAsSheet(BaseFragment parentFragment, boolean gallery, int type, CameraScanActivityDelegate cameraDelegate, boolean any) {
        if (parentFragment == null || parentFragment.getParentActivity() == null) {
            return null;
        }
        ActionBarLayout[] actionBarLayout = new ActionBarLayout[]{new ActionBarLayout(parentFragment.getParentActivity())};
        BottomSheet bottomSheet = new BottomSheet(parentFragment.getParentActivity(), false) {
            CameraScanActivity fragment;
            {
                actionBarLayout[0].init(new ArrayList<>());
                fragment = new CameraScanActivity(type) {
                    @Override
                    public void finishFragment() {
                        dismiss();
                    }

                    @Override
                    public void removeSelfFromStack() {
                        dismiss();
                    }
                };
                fragment.needGalleryButton = gallery;
                fragment.any = any;
                actionBarLayout[0].addFragmentToStack(fragment);
                actionBarLayout[0].showLastFragment();
                actionBarLayout[0].setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);
                fragment.setDelegate(cameraDelegate);
                containerView = actionBarLayout[0];
                setApplyBottomPadding(false);
                setApplyBottomPadding(false);
                setOnDismissListener(dialog -> fragment.onFragmentDestroy());
            }

            @Override
            protected boolean canDismissWithSwipe() {
                return false;
            }

            @Override
            public void onBackPressed() {
                if (actionBarLayout[0] == null || actionBarLayout[0].fragmentsStack.size() <= 1) {
                    super.onBackPressed();
                } else {
                    actionBarLayout[0].onBackPressed();
                }
            }

            @Override
            public void dismiss() {
                super.dismiss();
                actionBarLayout[0] = null;
            }
        };
        AndroidUtilities.setLightNavigationBar(bottomSheet.getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bottomSheet.getWindow().setNavigationBarColor(0xff000000);
        }
        bottomSheet.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        bottomSheet.show();
        return actionBarLayout;
    }

    public static ActionBarLayout[] showAsSheet(BaseFragment parentFragment, CameraScanActivityDelegate cameraDelegate) {
        return showAsSheet(parentFragment, true, TYPE_QR, cameraDelegate, true);
    }

    public CameraScanActivity(int type) {
        super();
        currentType = type;
        if (isQr()) {
            Utilities.globalQueue.postRunnable(() -> {
                qrReader = new QRCodeReader();
                //visionQrReader = new BarcodeDetector.Builder(ApplicationLoader.applicationContext).setBarcodeFormats(Barcode.QR_CODE).build();
            });
        }

        switch (SharedConfig.getDevicePerformanceClass()) {
            case SharedConfig.PERFORMANCE_CLASS_LOW:
                sps = 8;
                break;
            case SharedConfig.PERFORMANCE_CLASS_AVERAGE:
                sps = 24;
                break;
            case SharedConfig.PERFORMANCE_CLASS_HIGH:
            default:
                sps = 40;
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        destroy(false, null);
        if (getParentActivity() != null) {
            getParentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        /*if (visionQrReader != null) {
            visionQrReader.release();
        }*/
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarWhiteSelector), false);
        actionBar.setCastShadows(false);
        if (!AndroidUtilities.isTablet() && !isQr()) {
            actionBar.showActionModeTop();
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        paint.setColor(0x7f000000);
        cornerPaint.setColor(0xffffffff);
        cornerPaint.setStyle(Paint.Style.FILL);

        ViewGroup viewGroup = new ViewGroup(context) {

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int width = MeasureSpec.getSize(widthMeasureSpec);
                int height = MeasureSpec.getSize(heightMeasureSpec);
                actionBar.measure(widthMeasureSpec, heightMeasureSpec);
                if (currentType == TYPE_MRZ) {
                    if (cameraView != null) {
                        cameraView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) (width * 0.704f), MeasureSpec.EXACTLY));
                    }
                } else {
                    if (cameraView != null) {
                        cameraView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
                    }
                    recognizedMrzView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
                    if (galleryButton != null) {
                        galleryButton.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(60), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(60), MeasureSpec.EXACTLY));
                    }
                    flashButton.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(60), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(60), MeasureSpec.EXACTLY));
                }
                titleTextView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(72), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));
                descriptionText.measure(MeasureSpec.makeMeasureSpec((int) (width * 0.9f), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED));

                setMeasuredDimension(width, height);
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                int width = r - l;
                int height = b - t;

                int y = 0;
                if (currentType == TYPE_MRZ) {
                    if (cameraView != null) {
                        cameraView.layout(0, y, cameraView.getMeasuredWidth(), y + cameraView.getMeasuredHeight());
                    }
                    recognizedMrzView.setTextSize(TypedValue.COMPLEX_UNIT_PX, height / 22);
                    recognizedMrzView.setPadding(0, 0, 0, height / 15);
                    y = (int) (height * 0.65f);
                    titleTextView.layout(AndroidUtilities.dp(36), y, AndroidUtilities.dp(36) + titleTextView.getMeasuredWidth(), y + titleTextView.getMeasuredHeight());
                } else {
                    actionBar.layout(0, 0, actionBar.getMeasuredWidth(), actionBar.getMeasuredHeight());
                    if (cameraView != null) {
                        cameraView.layout(0, 0, cameraView.getMeasuredWidth(), cameraView.getMeasuredHeight());
                    }
                    int size = (int) (Math.min(width, height) / 1.5f);
                    if (currentType == TYPE_QR) {
                        y = (height - size) / 2 - titleTextView.getMeasuredHeight() - AndroidUtilities.dp(30);
                    } else {
                        y = (height - size) / 2 - titleTextView.getMeasuredHeight() - AndroidUtilities.dp(64);
                    }
                    titleTextView.layout(AndroidUtilities.dp(36), y, AndroidUtilities.dp(36) + titleTextView.getMeasuredWidth(), y + titleTextView.getMeasuredHeight());
                    recognizedMrzView.layout(0, getMeasuredHeight() - recognizedMrzView.getMeasuredHeight(), getMeasuredWidth(), getMeasuredHeight());

                    int x;
                    if (needGalleryButton) {
                        x = width / 2 + AndroidUtilities.dp(35);
                    } else {
                        x = width / 2 - flashButton.getMeasuredWidth() / 2;
                    }
                    y = (height - size) / 2 + size + AndroidUtilities.dp(80);
                    flashButton.layout(x, y, x + flashButton.getMeasuredWidth(), y + flashButton.getMeasuredHeight());

                    if (galleryButton != null) {
                        x = width / 2 - AndroidUtilities.dp(35) - galleryButton.getMeasuredWidth();
                        galleryButton.layout(x, y, x + galleryButton.getMeasuredWidth(), y + galleryButton.getMeasuredHeight());
                    }
                }

                y = (int) (height * 0.74f);
                int x = (int) (width * 0.05f);
                descriptionText.layout(x, y, x + descriptionText.getMeasuredWidth(), y + descriptionText.getMeasuredHeight());

                updateNormalBounds();
            }

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                boolean result = super.drawChild(canvas, child, drawingTime);
                if (isQr() && child == cameraView) {
                    RectF bounds = getBounds();
                    int sizex = (int) (child.getWidth() * bounds.width()),
                        sizey = (int) (child.getHeight() * bounds.height()),
                        cx = (int) (child.getWidth() * bounds.centerX()),
                        cy = (int) (child.getHeight() * bounds.centerY());

                    sizex *= (.5f + qrAppearingValue * .5f);
                    sizey *= (.5f + qrAppearingValue * .5f);
                    int x = cx - sizex / 2,
                        y = cy - sizey / 2;

                    paint.setAlpha((int) (255 * (1f - (1f - backShadowAlpha) * Math.min(1, qrAppearingValue))));
                    canvas.drawRect(0, 0, child.getMeasuredWidth(), y, paint);
                    canvas.drawRect(0, y + sizey, child.getMeasuredWidth(), child.getMeasuredHeight(), paint);
                    canvas.drawRect(0, y, x, y + sizey, paint);
                    canvas.drawRect(x + sizex, y, child.getMeasuredWidth(), y + sizey, paint);
                    paint.setAlpha((int) (255 * Math.max(0, 1f - qrAppearingValue)));
                    canvas.drawRect(x, y, x + sizex, y + sizey, paint);

                    final int lineWidth = AndroidUtilities.lerp(0, AndroidUtilities.dp(4), Math.min(1, qrAppearingValue * 20f)),
                              halfLineWidth = lineWidth / 2;
                    final int lineLength = AndroidUtilities.lerp(Math.min(sizex, sizey), AndroidUtilities.dp(20), Math.min(1.2f, (float) Math.pow(qrAppearingValue, 1.8f)));

                    cornerPaint.setAlpha((int) (255 * Math.min(1, qrAppearingValue)));

                    path.reset();
                    path.arcTo(aroundPoint(x, y + lineLength, halfLineWidth), 0, 180);
                    path.arcTo(aroundPoint((int) (x + lineWidth * 1.5f), (int) (y + lineWidth * 1.5f), lineWidth * 2), 180, 90);
                    path.arcTo(aroundPoint(x + lineLength, y, halfLineWidth), 270, 180);
                    path.lineTo(x + halfLineWidth, y + halfLineWidth);
                    path.arcTo(aroundPoint((int) (x + lineWidth * 1.5f), (int) (y + lineWidth * 1.5f), lineWidth), 270, -90);
                    path.close();
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.arcTo(aroundPoint(x + sizex, y + lineLength, halfLineWidth), 180, -180);
                    path.arcTo(aroundPoint((int) (x + sizex - lineWidth * 1.5f), (int) (y + lineWidth * 1.5f), lineWidth * 2), 0, -90);
                    path.arcTo(aroundPoint(x + sizex- lineLength, y, halfLineWidth), 270, -180);
                    path.arcTo(aroundPoint((int) (x + sizex - lineWidth * 1.5f), (int) (y + lineWidth * 1.5f), lineWidth), 270, 90);
                    path.close();
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.arcTo(aroundPoint(x, y + sizey - lineLength, halfLineWidth), 0, -180);
                    path.arcTo(aroundPoint((int) (x + lineWidth * 1.5f), (int) (y + sizey - lineWidth * 1.5f), lineWidth * 2), 180, -90);
                    path.arcTo(aroundPoint(x + lineLength, y + sizey, halfLineWidth), 90, -180);
                    path.arcTo(aroundPoint((int) (x + lineWidth * 1.5f), (int) (y + sizey - lineWidth * 1.5f), lineWidth), 90, 90);
                    path.close();
                    canvas.drawPath(path, cornerPaint);

                    path.reset();
                    path.arcTo(aroundPoint(x + sizex, y + sizey - lineLength, halfLineWidth), 180, 180);
                    path.arcTo(aroundPoint((int) (x + sizex - lineWidth * 1.5f), (int) (y + sizey - lineWidth * 1.5f), lineWidth * 2), 0, 90);
                    path.arcTo(aroundPoint(x + sizex - lineLength, y + sizey, halfLineWidth), 90, 180);
                    path.arcTo(aroundPoint((int) (x + sizex - lineWidth * 1.5f), (int) (y + sizey - lineWidth * 1.5f), lineWidth), 90, -90);
                    path.close();

                    canvas.drawPath(path, cornerPaint);
                }
                return result;
            }

            private RectF aroundPoint(int x, int y, int r) {
                AndroidUtilities.rectTmp.set(x - r, y - r, x + r, y + r);
                return AndroidUtilities.rectTmp;
            }
        };
        viewGroup.setOnTouchListener((v, event) -> true);
        fragmentView = viewGroup;

        if (currentType == TYPE_QR || currentType == TYPE_QR_LOGIN) {
            fragmentView.postDelayed(this::initCameraView, 200);
        } else {
            initCameraView();
        }

        if (currentType == TYPE_MRZ) {
            actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        } else {
            actionBar.setBackgroundDrawable(null);
            actionBar.setAddToContainer(false);
            actionBar.setItemsColor(0xffffffff, false);
            actionBar.setItemsBackgroundColor(0x22ffffff, false);
            viewGroup.setBackgroundColor(Theme.getColor(Theme.key_wallet_blackBackground));
            viewGroup.addView(actionBar);
        }

        if (currentType == TYPE_QR_LOGIN) {
            actionBar.setTitle(LocaleController.getString("AuthAnotherClientScan", R.string.AuthAnotherClientScan));
        }

        Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectionPaint.setPathEffect(LinkPath.roundedEffect);
        selectionPaint.setColor(ColorUtils.setAlphaComponent(Color.WHITE, 50));
        titleTextView = new TextView(context) {
            LinkPath textPath;

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                if (getText() instanceof Spanned) {
                    Spanned spanned = (Spanned) getText();
                    URLSpanNoUnderline[] innerSpans = spanned.getSpans(0, spanned.length(), URLSpanNoUnderline.class);
                    if (innerSpans != null && innerSpans.length > 0) {
                        textPath = new LinkPath(true);
                        textPath.setAllowReset(false);
                        for (int a = 0; a < innerSpans.length; a++) {
                            int start = spanned.getSpanStart(innerSpans[a]);
                            int end = spanned.getSpanEnd(innerSpans[a]);
                            textPath.setCurrentLayout(getLayout(), start, 0);
                            int shift = getText() != null ? getPaint().baselineShift : 0;
                            textPath.setBaselineShift(shift != 0 ? shift + AndroidUtilities.dp(shift > 0 ? 5 : -2) : 0);
                            getLayout().getSelectionPath(start, end, textPath);
                        }
                        textPath.setAllowReset(true);
                    }
                }
            }

            @Override
            protected void onDraw(Canvas canvas) {
                if (textPath != null) {
                    canvas.drawPath(textPath, selectionPaint);
                }
                super.onDraw(canvas);
            }
        };
        titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);

        viewGroup.addView(titleTextView);

        descriptionText = new TextView(context);
        descriptionText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText6));
        descriptionText.setGravity(Gravity.CENTER_HORIZONTAL);
        descriptionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        viewGroup.addView(descriptionText);

        recognizedMrzView = new TextView(context);
        recognizedMrzView.setTextColor(0xffffffff);
        recognizedMrzView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        recognizedMrzView.setAlpha(0);

        if (currentType == TYPE_MRZ) {
            titleTextView.setText(LocaleController.getString("PassportScanPassport", R.string.PassportScanPassport));
            descriptionText.setText(LocaleController.getString("PassportScanPassportInfo", R.string.PassportScanPassportInfo));
            titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            recognizedMrzView.setTypeface(Typeface.MONOSPACE);
        } else {
            if (needGalleryButton) {
                //titleTextView.setText(LocaleController.getString("WalletScanCode", R.string.WalletScanCode));
            } else {
                if (currentType == TYPE_QR) {
                    titleTextView.setText(LocaleController.getString("AuthAnotherClientScan", R.string.AuthAnotherClientScan));
                } else {
                    String text = LocaleController.getString("AuthAnotherClientInfo5", R.string.AuthAnotherClientInfo5);
                    SpannableStringBuilder spanned = new SpannableStringBuilder(text);

                    String[] links = new String[] {
                        LocaleController.getString("AuthAnotherWebClientUrl", R.string.AuthAnotherWebClientUrl),
                        LocaleController.getString("AuthAnotherClientDownloadClientUrl", R.string.AuthAnotherClientDownloadClientUrl)
                    };
                    for (int i = 0; i < links.length; ++i) {
                        text = spanned.toString();
                        int index1 = text.indexOf('*');
                        int index2 = text.indexOf('*', index1 + 1);

                        if (index1 != -1 && index2 != -1 && index1 != index2) {
                            titleTextView.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
                            spanned.replace(index2, index2 + 1, " ");
                            spanned.replace(index1, index1 + 1, " ");
                            index1 += 1;
                            index2 += 1;
                            spanned.setSpan(new URLSpanNoUnderline(links[i]), index1, index2 - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            spanned.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf")), index1, index2 - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else {
                            break;
                        }
                    }

                    titleTextView.setLinkTextColor(Color.WHITE);
                    titleTextView.setHighlightColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkSelection));

                    titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                    titleTextView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
                    titleTextView.setPadding(0, 0, 0, 0);
                    titleTextView.setText(spanned);
                }
            }
            titleTextView.setTextColor(0xffffffff);
            recognizedMrzView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            recognizedMrzView.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), AndroidUtilities.dp(10));
            if (needGalleryButton) {
                //recognizedMrzView.setText(LocaleController.getString("WalletScanCodeNotFound", R.string.WalletScanCodeNotFound));
            } else {
                recognizedMrzView.setText(LocaleController.getString("AuthAnotherClientNotFound", R.string.AuthAnotherClientNotFound));
            }
            viewGroup.addView(recognizedMrzView);

            if (needGalleryButton) {
                galleryButton = new ImageView(context);
                galleryButton.setScaleType(ImageView.ScaleType.CENTER);
                galleryButton.setImageResource(R.drawable.qr_gallery);
                galleryButton.setBackgroundDrawable(Theme.createSelectorDrawableFromDrawables(Theme.createCircleDrawable(AndroidUtilities.dp(60), 0x22ffffff), Theme.createCircleDrawable(AndroidUtilities.dp(60), 0x44ffffff)));
                viewGroup.addView(galleryButton);
                galleryButton.setOnClickListener(currentImage -> {
                    if (getParentActivity() == null) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (getParentActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            getParentActivity().requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE);
                            return;
                        }
                    }
                    PhotoAlbumPickerActivity fragment = new PhotoAlbumPickerActivity(PhotoAlbumPickerActivity.SELECT_TYPE_QR, false, false, null);
                    fragment.setMaxSelectedPhotos(1, false);
                    fragment.setAllowSearchImages(false);
                    fragment.setDelegate(new PhotoAlbumPickerActivity.PhotoAlbumPickerActivityDelegate() {
                        @Override
                        public void didSelectPhotos(ArrayList<SendMessagesHelper.SendingMediaInfo> photos, boolean notify, int scheduleDate) {
                            try {
                                if (!photos.isEmpty()) {
                                    SendMessagesHelper.SendingMediaInfo info = photos.get(0);
                                    if (info.path != null) {
                                        Point screenSize = AndroidUtilities.getRealScreenSize();
                                        Bitmap bitmap = ImageLoader.loadBitmap(info.path, null, screenSize.x, screenSize.y, true);
                                        QrResult res = tryReadQr(null, null, 0, 0, 0, bitmap);
                                        if (res != null) {
                                            if (delegate != null) {
                                                delegate.didFindQr(res.text);
                                            }
                                            removeSelfFromStack();
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                FileLog.e(e);
                            }
                        }

                        @Override
                        public void startPhotoSelectActivity() {
                            try {
                                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                                photoPickerIntent.setType("image/*");
                                getParentActivity().startActivityForResult(photoPickerIntent, 11);
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                        }
                    });
                    presentFragment(fragment);
                });
            }

            flashButton = new ImageView(context);
            flashButton.setScaleType(ImageView.ScaleType.CENTER);
            flashButton.setImageResource(R.drawable.qr_flashlight);
            flashButton.setBackgroundDrawable(Theme.createCircleDrawable(AndroidUtilities.dp(60), 0x22ffffff));
            viewGroup.addView(flashButton);
            flashButton.setOnClickListener(currentImage -> {
                if (cameraView == null) {
                    return;
                }
                CameraSession session = cameraView.getCameraSession();
                if (session != null) {
                    ShapeDrawable shapeDrawable = (ShapeDrawable) flashButton.getBackground();
                    if (flashAnimator != null) {
                        flashAnimator.cancel();
                        flashAnimator = null;
                    }
                    flashAnimator = new AnimatorSet();
                    ObjectAnimator animator = ObjectAnimator.ofInt(shapeDrawable, AnimationProperties.SHAPE_DRAWABLE_ALPHA, flashButton.getTag() == null ? 0x44 : 0x22);
                    animator.addUpdateListener(animation -> flashButton.invalidate());
                    flashAnimator.playTogether(animator);
                    flashAnimator.setDuration(200);
                    flashAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                    flashAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            flashAnimator = null;
                        }
                    });
                    flashAnimator.start();
                    if (flashButton.getTag() == null) {
                        flashButton.setTag(1);
                        session.setTorchEnabled(true);
                    } else {
                        flashButton.setTag(null);
                        session.setTorchEnabled(false);
                    }
                }
            });
        }

        if (getParentActivity() != null) {
            getParentActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        fragmentView.setKeepScreenOn(true);

        return fragmentView;
    }

    private ValueAnimator recognizedAnimator;
    private float recognizedT = 0;
    private SpringAnimation useRecognizedBoundsAnimator;
    private float useRecognizedBounds = 0;
    private void updateRecognized() {
        if (recognizedAnimator != null) {
            recognizedAnimator.cancel();
        }
        float newRecognizedT = recognized ? 1f : 0f;
        recognizedAnimator = ValueAnimator.ofFloat(recognizedT, newRecognizedT);
        recognizedAnimator.addUpdateListener(a -> {
            recognizedT = (float) a.getAnimatedValue();
            titleTextView.setAlpha(1f - recognizedT);
            flashButton.setAlpha(1f - recognizedT);
            backShadowAlpha = .5f + recognizedT * .25f;
            fragmentView.invalidate();
        });
        recognizedAnimator.setDuration((long) (300 * Math.abs(recognizedT - newRecognizedT)));
        recognizedAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
        recognizedAnimator.start();

        if (useRecognizedBoundsAnimator != null) {
            useRecognizedBoundsAnimator.cancel();
        }
        final float force = 500f;
        useRecognizedBoundsAnimator = new SpringAnimation(new FloatValueHolder((recognized ? useRecognizedBounds : 1f - useRecognizedBounds) * force));
        useRecognizedBoundsAnimator.addUpdateListener((animation, value, velocity) -> {
            useRecognizedBounds = recognized ? value / force : (1f - value / force);
            fragmentView.invalidate();
        });
        useRecognizedBoundsAnimator.setSpring(new SpringForce(force));
        useRecognizedBoundsAnimator.getSpring().setDampingRatio(1f);
        useRecognizedBoundsAnimator.getSpring().setStiffness(500.0f);
        useRecognizedBoundsAnimator.start();
    }

    private void initCameraView() {
        if (fragmentView == null) {
            return;
        }
        CameraController.getInstance().initCamera(null);
        cameraView = new CameraView(fragmentView.getContext(), false);
        cameraView.setUseMaxPreview(true);
        cameraView.setOptimizeForBarcode(true);
        cameraView.setDelegate(() -> {
            startRecognizing();
            if (isQr()) {
                if (qrAppearing != null) {
                    qrAppearing.cancel();
                    qrAppearing = null;
                }

                qrAppearing = new SpringAnimation(new FloatValueHolder(0));
                qrAppearing.addUpdateListener((animation, value, velocity) -> {
                    qrAppearingValue = value / 500f;
                    fragmentView.invalidate();
                });
                qrAppearing.addEndListener((animation, canceled, value, velocity) -> {
                    if (qrAppearing != null) {
                        qrAppearing.cancel();
                        qrAppearing = null;
                    }
                });
                qrAppearing.setSpring(new SpringForce(500f));
                qrAppearing.getSpring().setDampingRatio(0.8f);
                qrAppearing.getSpring().setStiffness(250.0f);
                qrAppearing.start();
            }
        });
        ((ViewGroup) fragmentView).addView(cameraView, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        if (currentType == TYPE_MRZ && recognizedMrzView != null) {
            cameraView.addView(recognizedMrzView);
        }
    }

    private void updateRecognizedBounds(RectF newBounds) {
        final long now = SystemClock.elapsedRealtime();
        if (lastBoundsUpdate == 0) {
            // first update = set
            lastBoundsUpdate = now - boundsUpdateDuration;
            bounds.set(newBounds);
            fromBounds.set(newBounds);
        } else {
            // next updates = interpolate
            if (fromBounds != null && now - lastBoundsUpdate < boundsUpdateDuration) {
                float t = (now - lastBoundsUpdate) / (float) boundsUpdateDuration;
                t = Math.min(1, Math.max(0, t));
                AndroidUtilities.lerp(fromBounds, bounds, t, fromBounds);
            } else {
                if (fromBounds == null) {
                    fromBounds = new RectF();
                }
                fromBounds.set(bounds);
            }
            bounds.set(newBounds);
            lastBoundsUpdate = now;
        }
        fragmentView.invalidate();
    }

    private RectF getRecognizedBounds() {
        if (fromBounds == null) {
            return bounds;
        } else {
            float t = (SystemClock.elapsedRealtime() - lastBoundsUpdate) / (float) boundsUpdateDuration;
            t = Math.min(1, Math.max(0, t));
            if (t < 1f) {
                fragmentView.invalidate();
            }
            AndroidUtilities.lerp(fromBounds, bounds, t, AndroidUtilities.rectTmp);
            return AndroidUtilities.rectTmp;
        }
    }

    private RectF normalBounds;
    private void updateNormalBounds() {
        if (normalBounds == null) {
            normalBounds = new RectF();
        }
        int width = AndroidUtilities.displaySize.x,
                height = AndroidUtilities.displaySize.y,
                side = (int) (Math.min(width, height) / 1.5f);
        normalBounds.set(
            (width - side) / 2f / (float) width,
            (height - side) / 2f / (float) height,
            (width + side) / 2f / (float) width,
            (height + side) / 2f / (float) height
        );
    }
    private RectF getBounds() {
        RectF recognizedBounds = getRecognizedBounds();
        if (useRecognizedBounds < 1f) {
            if (normalBounds == null) {
                updateNormalBounds();
            }
            AndroidUtilities.lerp(normalBounds, recognizedBounds, useRecognizedBounds, recognizedBounds);
        }
        return recognizedBounds;
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == 11 && data != null && data.getData() != null) {
            try {
                Point screenSize = AndroidUtilities.getRealScreenSize();
                Bitmap bitmap = ImageLoader.loadBitmap(null, data.getData(), screenSize.x, screenSize.y, true);
                QrResult res = tryReadQr(null, null, 0, 0, 0, bitmap);
                if (res != null) {
                    if (delegate != null) {
                        delegate.didFindQr(res.text);
                    }
                    finishFragment();
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
    }

    public void setDelegate(CameraScanActivityDelegate cameraScanActivityDelegate) {
        delegate = cameraScanActivityDelegate;
    }

    public void destroy(boolean async, final Runnable beforeDestroyRunnable) {
        if (cameraView != null) {
            cameraView.destroy(async, beforeDestroyRunnable);
            cameraView = null;
        }
        backgroundHandlerThread.quitSafely();
    }

    private Runnable requestShot = new Runnable() {
        @Override
        public void run() {
            if (cameraView != null && !recognized && cameraView.getCameraSession() != null) {
                handler.post(() -> {
                    if (cameraView != null) {
                        processShot(cameraView.getTextureView().getBitmap());
                    }
                });
            }
        }
    };

    private void startRecognizing() {
        backgroundHandlerThread.start();
        handler = new Handler(backgroundHandlerThread.getLooper());
        AndroidUtilities.runOnUIThread(requestShot, 0);
    }

    private void onNoQrFound() {
        AndroidUtilities.runOnUIThread(() -> {
            if (recognizedMrzView.getTag() != null) {
                recognizedMrzView.setTag(null);
                recognizedMrzView.animate().setDuration(200).alpha(0.0f).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
            }
        });
    }

    private float averageProcessTime = 0;
    private long processTimesCount = 0;
    public void processShot(Bitmap bitmap) {
        if (cameraView == null) {
            return;
        }
        final long from = SystemClock.elapsedRealtime();
        try {
            Size size = cameraView.getPreviewSize();
            if (currentType == TYPE_MRZ) {
                final MrzRecognizer.Result res = MrzRecognizer.recognize(bitmap, false);
                if (res != null && !TextUtils.isEmpty(res.firstName) && !TextUtils.isEmpty(res.lastName) && !TextUtils.isEmpty(res.number) && res.birthDay != 0 && (res.expiryDay != 0 || res.doesNotExpire) && res.gender != MrzRecognizer.Result.GENDER_UNKNOWN) {
                    recognized = true;
                    CameraController.getInstance().stopPreview(cameraView.getCameraSession());
                    AndroidUtilities.runOnUIThread(() -> {
                        recognizedMrzView.setText(res.rawMRZ);
                        recognizedMrzView.animate().setDuration(200).alpha(1f).setInterpolator(CubicBezierInterpolator.DEFAULT).start();
                        if (delegate != null) {
                            delegate.didFindMrzInfo(res);
                        }
                        AndroidUtilities.runOnUIThread(this::finishFragment, 1200);
                    });
                    return;
                }
            } else {
                int side = (int) (Math.min(size.getWidth(), size.getHeight()) / 1.5f);
                int x = (size.getWidth() - side) / 2;
                int y = (size.getHeight() - side) / 2;

                QrResult res = tryReadQr(null, size, x, y, side, bitmap);
                if (recognized) {
                    recognizeIndex++;
                }
                if (res != null) {
                    recognizeFailed = 0;
                    recognizedText = res.text;
                    if (!recognized) {
                        recognized = true;
                        qrLoading = delegate.processQr(recognizedText, () -> {
                            if (cameraView != null && cameraView.getCameraSession() != null) {
                                CameraController.getInstance().stopPreview(cameraView.getCameraSession());
                            }
                            AndroidUtilities.runOnUIThread(() -> {
                                if (delegate != null) {
                                    delegate.didFindQr(recognizedText);
                                }
                                finishFragment();
                            });
                        });
                        recognizedStart = SystemClock.elapsedRealtime();
                        AndroidUtilities.runOnUIThread(this::updateRecognized);
                    }
                    AndroidUtilities.runOnUIThread(() -> updateRecognizedBounds(res.bounds));
                } else if (recognized) {
                    recognizeFailed++;
                    if (recognizeFailed > 4 && !qrLoading) {
                        recognized = false;
                        recognizeIndex = 0;
                        recognizedText = null;
                        AndroidUtilities.runOnUIThread(this::updateRecognized);
                        AndroidUtilities.runOnUIThread(requestShot, 500);
                        return;
                    }
                }

                if (( // finish because...
                      (recognizeIndex == 0 && res != null && res.bounds == null && !qrLoading) || // first recognition doesn't have bounds
                      (SystemClock.elapsedRealtime() - recognizedStart > 1000 && !qrLoading) // got more than 1 second and nothing is loading
                    ) && recognizedText != null) {
                    if (cameraView != null && cameraView.getCameraSession() != null) {
                        CameraController.getInstance().stopPreview(cameraView.getCameraSession());
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        if (delegate != null) {
                            delegate.didFindQr(recognizedText);
                        }
                        finishFragment();
                    });
                } else if (recognized) {
                    long delay = Math.max(16, 1000 / sps - (long) averageProcessTime);
                    handler.postDelayed(() -> {
                        if (cameraView != null) {
                            processShot(cameraView.getTextureView().getBitmap());
                        }
                    }, delay);
                }
            }
        } catch (Throwable ignore) {
            onNoQrFound();
        }
        final long to = SystemClock.elapsedRealtime();
        long timeout = to - from;
        averageProcessTime = (averageProcessTime * processTimesCount + timeout) / (++processTimesCount);
        processTimesCount = Math.max(processTimesCount, 30);

        if (!recognized) {
            AndroidUtilities.runOnUIThread(requestShot, 500);
        }
    }

    private class QrResult {
        String text;
        RectF bounds;

        public QrResult(String text, RectF bounds) {
            this.text = text;
            this.bounds = bounds;
        }

        public QrResult() {}
    }

    private QrResult tryReadQr(byte[] data, Size size, int x, int y, int side, Bitmap bitmap) {
        try {
            String text;
            RectF bounds = new RectF();
            int width = 1, height = 1;
            /*if (visionQrReader != null && visionQrReader.isOperational()) {
                Frame frame;
                if (bitmap != null) {
                    frame = new Frame.Builder().setBitmap(bitmap).build();
                    width = bitmap.getWidth();
                    height = bitmap.getHeight();
                } else {
                    frame = new Frame.Builder().setImageData(ByteBuffer.wrap(data), size.getWidth(), size.getHeight(), ImageFormat.NV21).build();
                    width = size.getWidth();
                    height = size.getWidth();
                }
                SparseArray<Barcode> codes = visionQrReader.detect(frame);
                if (codes != null && codes.size() > 0) {
                    Barcode code = codes.valueAt(0);
                    text = code.rawValue;
                    if (code.cornerPoints == null || code.cornerPoints.length == 0) {
                        bounds = null;
                    } else {
//                        bounds.set(code.getBoundingBox());
                        float minX = Float.MAX_VALUE,
                              maxX = Float.MIN_VALUE,
                              minY = Float.MAX_VALUE,
                              maxY = Float.MIN_VALUE;
                        for (Point point : code.cornerPoints) {
                            minX = Math.min(minX, point.x);
                            maxX = Math.max(maxX, point.x);
                            minY = Math.min(minY, point.y);
                            maxY = Math.max(maxY, point.y);
                        }
                        bounds.set(minX, minY, maxX, maxY);
                    }
                } else {
                    text = null;
                }
            } else */
            // NekoX: Remove google parts visionQrReader

            if (qrReader != null) {
                LuminanceSource source;
                if (bitmap != null) {
                    int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
                    bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
                    width = bitmap.getWidth();
                    height = bitmap.getWidth();
                } else {
                    source = new PlanarYUVLuminanceSource(data, size.getWidth(), size.getHeight(), x, y, side, side, false);
                    width = size.getWidth();
                    height = size.getHeight();
                }

                Result result = qrReader.decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
                if (result == null) {
                    onNoQrFound();
                    return null;
                }
                text = result.getText();
                if (result.getResultPoints() == null || result.getResultPoints().length == 0) {
                    bounds = null;
                } else {
                    float minX = Float.MAX_VALUE,
                          maxX = Float.MIN_VALUE,
                          minY = Float.MAX_VALUE,
                          maxY = Float.MIN_VALUE;
                    for (ResultPoint point : result.getResultPoints()) {
                        minX = Math.min(minX, point.getX());
                        maxX = Math.max(maxX, point.getX());
                        minY = Math.min(minY, point.getY());
                        maxY = Math.max(maxY, point.getY());
                    }
                    bounds.set(minX, minY, maxX, maxY);
                }
            } else {
                text = null;
            }
            if (TextUtils.isEmpty(text)) {
                onNoQrFound();
                return null;
            }
            if (any) return new QrResult(text, bounds);
            if (needGalleryButton) {
                if (!text.startsWith("ton://transfer/")) {
                    //onNoWalletFound(bitmap != null);
                    return null;
                }
                Uri uri = Uri.parse(text);
                String path = uri.getPath().replace("/", "");
            }
            QrResult qrResult = new QrResult();
            if (bounds != null) {
                int paddingx = AndroidUtilities.dp(25),
                    paddingy = AndroidUtilities.dp(15);
                bounds.set(bounds.left - paddingx, bounds.top - paddingy, bounds.right + paddingx, bounds.bottom + paddingy);
                bounds.set(
                    bounds.left / (float) width, bounds.top / (float) height,
                    bounds.right / (float) width, bounds.bottom / (float) height
                );
            }
            qrResult.bounds = bounds;
            qrResult.text = text;
            return qrResult;
        } catch (Throwable ignore) {
            onNoQrFound();
        }
        return null;
    }


    private boolean isQr() {
        return currentType == TYPE_QR || currentType == TYPE_QR_LOGIN;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        if (isQr()) {
            return themeDescriptions;
        }

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarWhiteSelector));

        themeDescriptions.add(new ThemeDescription(titleTextView, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(descriptionText, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteGrayText6));

        return themeDescriptions;
    }
}
