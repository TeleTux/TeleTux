package tw.nekomimi.nekogram.ui;

import android.graphics.Paint;
import android.text.TextPaint;
import tw.nekomimi.nekogram.ui.SuperTextPaint;

import org.telegram.messenger.AndroidUtilities;

public class SuperTextPaint extends TextPaint {

    public SuperTextPaint() {
        super();
        setTypeface(AndroidUtilities.getTypeface("fonts/Vazirmatn-Regular.ttf"));
    }

    public SuperTextPaint(int flags) {
        super(flags);
        setTypeface(AndroidUtilities.getTypeface("fonts/Vazirmatn-Regular.ttf"));
    }

    public SuperTextPaint(Paint p) {
        super(p);
        setTypeface(AndroidUtilities.getTypeface("fonts/Vazirmatn-Regular.ttf"));
    }
}
