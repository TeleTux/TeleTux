package org.telegram.ui.Components.voip;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;

public class CellFlickerDrawable {

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader gradientShader;

    private Paint paintOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Shader gradientShader2;
    int size;

    int parentWidth;
    float progress;
    long lastUpdateTime;

    Matrix matrix = new Matrix();

    public boolean repeatEnabled = true;
    public boolean drawFrame = true;
    public float repeatProgress = 1.2f;
    public float animationSpeedScale = 1f;

    public CellFlickerDrawable() {
        this(64, 204);
    }
    public CellFlickerDrawable(int a1, int a2) {
        size = AndroidUtilities.dp(160);
        gradientShader = new LinearGradient(0, 0, size, 0, new int[]{Color.TRANSPARENT, ColorUtils.setAlphaComponent(Color.WHITE, a1), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP);
        gradientShader2 = new LinearGradient(0, 0, size, 0, new int[]{Color.TRANSPARENT, ColorUtils.setAlphaComponent(Color.WHITE, a2), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP);
        paint.setShader(gradientShader);
        paintOutline.setShader(gradientShader2);
        paintOutline.setStyle(Paint.Style.STROKE);
        paintOutline.setStrokeWidth(AndroidUtilities.dp(2));
    }

    public void setColors(int color) {
        setColors(color, 64, 204);
    }

    public void setColors(int color, int alpha1, int alpha2) {
        gradientShader = new LinearGradient(0, 0, size, 0, new int[]{Color.TRANSPARENT, ColorUtils.setAlphaComponent(color, alpha1), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP);
        gradientShader2 = new LinearGradient(0, 0, size, 0, new int[]{Color.TRANSPARENT, ColorUtils.setAlphaComponent(color, alpha2), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP);
        paint.setShader(gradientShader);
        paintOutline.setShader(gradientShader2);
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void draw(Canvas canvas, RectF rectF, float rad) {
        if (progress > 1f && !repeatEnabled) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime != 0) {
            long dt = currentTime - lastUpdateTime;
            if (dt > 10) {
                progress += (dt / 1200f) * animationSpeedScale;
                if (progress > repeatProgress) {
                    progress = 0;
                }
                lastUpdateTime = currentTime;
            }
        } else {
            lastUpdateTime = currentTime;
        }

        if (progress > 1f) {
            return;
        }

        float x = (parentWidth + size * 2) * progress - size;
        matrix.setTranslate(x, 0);
        gradientShader.setLocalMatrix(matrix);
        gradientShader2.setLocalMatrix(matrix);

        canvas.drawRoundRect(rectF, rad, rad, paint);
        if (drawFrame) {
            canvas.drawRoundRect(rectF, rad, rad, paintOutline);
        }
    }


    public void draw(Canvas canvas, GroupCallMiniTextureView view) {
        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime != 0) {
            long dt = currentTime - lastUpdateTime;
            if (dt > 10) {
                progress += dt / 500f;
                if (progress > 4f) {
                    progress = 0;
                }
                lastUpdateTime = currentTime;
            }
        } else {
            lastUpdateTime = currentTime;
        }

        if (progress > 1f) {
            return;
        }

        float x = (parentWidth + size * 2) * progress - size - view.getX();
        matrix.setTranslate(x, 0);
        gradientShader.setLocalMatrix(matrix);
        gradientShader2.setLocalMatrix(matrix);

        AndroidUtilities.rectTmp.set(view.textureView.currentClipHorizontal, view.textureView.currentClipVertical, view.textureView.getMeasuredWidth() - view.textureView.currentClipHorizontal, view.textureView.getMeasuredHeight() - view.textureView.currentClipVertical);
        canvas.drawRect(AndroidUtilities.rectTmp, paint);
        if (drawFrame) {
            canvas.drawRoundRect(AndroidUtilities.rectTmp, view.textureView.roundRadius, view.textureView.roundRadius, paintOutline);
        }
    }

    public void setParentWidth(int parentWidth) {
        this.parentWidth = parentWidth;
    }
}
