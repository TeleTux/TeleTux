package tw.nekomimi.nekogram.ui;

import android.content.Context;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;

public class SuperTextView extends TextView {

    public SuperTextView(Context context) {
        super(context);
        setTypeface(AndroidUtilities.getTypeface("fonts/Vazir-Regular"));
    }
}