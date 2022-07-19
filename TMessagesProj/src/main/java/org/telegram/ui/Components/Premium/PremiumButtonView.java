package org.telegram.ui.Components.Premium;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.voip.CellFlickerDrawable;

public class PremiumButtonView extends FrameLayout {

    private Paint paintOverlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float progress;
    private boolean inc;
    public TextView buttonTextView;
    public TextView overlayTextView;

    private boolean showOverlay;
    private float overlayProgress;
    public FrameLayout buttonLayout;
    ValueAnimator overlayAnimator;

    Path path = new Path();
    CellFlickerDrawable flickerDrawable;
    private boolean drawOverlayColor;

    RLottieImageView iconView;

    public PremiumButtonView(@NonNull Context context, boolean createOverlayTextView) {
        super(context);
        flickerDrawable = new CellFlickerDrawable();
        flickerDrawable.animationSpeedScale = 1.2f;
        flickerDrawable.drawFrame = false;
        flickerDrawable.repeatProgress = 4f;
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonTextView = new TextView(context);
        buttonTextView.setGravity(Gravity.CENTER);
        buttonTextView.setTextColor(Color.WHITE);
        buttonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        buttonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        iconView = new RLottieImageView(context);
        iconView.setColorFilter(Color.WHITE);
        iconView.setVisibility(View.GONE);

        buttonLayout = new FrameLayout(context);
        buttonLayout.addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));
        buttonLayout.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Color.TRANSPARENT, ColorUtils.setAlphaComponent(Color.WHITE, 120)));

        linearLayout.addView(buttonTextView, LayoutHelper.createLinear(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL));
        linearLayout.addView(iconView, LayoutHelper.createLinear(24, 24, 0, Gravity.CENTER_VERTICAL, 4, 0, 0, 0));
        addView(buttonLayout);

        if (createOverlayTextView) {
            overlayTextView = new TextView(context);
            overlayTextView.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
            overlayTextView.setGravity(Gravity.CENTER);
            overlayTextView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
            overlayTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            overlayTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            overlayTextView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(8), Color.TRANSPARENT, ColorUtils.setAlphaComponent(Color.WHITE, 120)));
            addView(overlayTextView);

            paintOverlayPaint.setColor(Theme.getColor(Theme.key_featuredStickers_addButton));
            updateOverlayProgress();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        if (overlayProgress != 1f || !drawOverlayColor) {
            if (inc) {
                progress += 16f / 1000f;
                if (progress > 3) {
                    inc = false;
                }
            } else {
                progress -= 16f / 1000f;
                if (progress < 1) {
                    inc = true;
                }
            }
            PremiumGradient.getInstance().updateMainGradientMatrix(0, 0, getMeasuredWidth(), getMeasuredHeight(), -getMeasuredWidth() * 0.1f * progress, 0);
            canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(8), AndroidUtilities.dp(8), PremiumGradient.getInstance().getMainGradientPaint());
            invalidate();
        }

        if (!BuildVars.IS_BILLING_UNAVAILABLE) {
            flickerDrawable.setParentWidth(getMeasuredWidth());
            flickerDrawable.draw(canvas, AndroidUtilities.rectTmp, AndroidUtilities.dp(8), null);
        }

        if (overlayProgress != 0 && drawOverlayColor) {
            paintOverlayPaint.setAlpha((int) (255 * overlayProgress));
            if (overlayProgress != 1f) {
                path.rewind();
                path.addCircle(getMeasuredWidth() / 2f, getMeasuredHeight() / 2f, Math.max(getMeasuredWidth(), getMeasuredHeight()) * 1.4f * overlayProgress, Path.Direction.CW);
                canvas.save();
                canvas.clipPath(path);
                canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(8), AndroidUtilities.dp(8), paintOverlayPaint);
                canvas.restore();
            } else {
                canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(8), AndroidUtilities.dp(8), paintOverlayPaint);
            }

        }

        super.dispatchDraw(canvas);
    }

    public void setOverlayText(String text, boolean drawOverlayColor, boolean animated) {
        showOverlay = true;
        this.drawOverlayColor = drawOverlayColor;
        overlayTextView.setText(text);
        updateOverlay(animated);
    }


    private void updateOverlay(boolean animated) {
        if (overlayAnimator != null) {
            overlayAnimator.removeAllListeners();
            overlayAnimator.cancel();
        }
        if (!animated) {
            overlayProgress = showOverlay ? 1f : 0;
            updateOverlayProgress();
            return;
        }
        overlayAnimator = ValueAnimator.ofFloat(overlayProgress, showOverlay ? 1f : 0);
        overlayAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                overlayProgress = (float) animation.getAnimatedValue();
                updateOverlayProgress();
            }
        });
        overlayAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlayProgress = showOverlay ? 1f : 0f;
                updateOverlayProgress();
            }
        });
        overlayAnimator.setDuration(250);
        overlayAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
        overlayAnimator.start();
    }

    private void updateOverlayProgress() {
        overlayTextView.setAlpha(overlayProgress);
        overlayTextView.setTranslationY(AndroidUtilities.dp(12) * (1f - overlayProgress));
        buttonLayout.setAlpha(1f - overlayProgress);
        buttonLayout.setTranslationY(-AndroidUtilities.dp(12) * (overlayProgress));
        buttonLayout.setVisibility(overlayProgress == 1f ? View.INVISIBLE : View.VISIBLE);
        overlayTextView.setVisibility(overlayProgress == 0 ? View.INVISIBLE : View.VISIBLE);
        invalidate();
    }

    public void clearOverlayText() {
        showOverlay = false;
        updateOverlay(true);
    }

    public void setIcon(int id) {
        iconView.setAnimation(id, 24, 24);
        flickerDrawable.progress = 2f;
        flickerDrawable.setOnRestartCallback(() -> {
            iconView.getAnimatedDrawable().setCurrentFrame(0, true);
            iconView.playAnimation();
        });
        invalidate();
        iconView.setVisibility(View.VISIBLE);
    }

    public void hideIcon() {
        flickerDrawable.setOnRestartCallback(null);
        iconView.setVisibility(View.GONE);
    }

    public void setButton(String text, View.OnClickListener clickListener) {
        buttonTextView.setText(text);
        buttonLayout.setOnClickListener(clickListener);
    }
}
