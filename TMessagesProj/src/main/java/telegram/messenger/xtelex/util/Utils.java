package telegram.messenger.xtelex.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import telegram.messenger.xtelex.ApplicationLoader;

public class Utils {

    public static void shareAlert(Context context, String title, String message) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, message);
        context.startActivity(Intent.createChooser(share, title));
    }


    public static void log(String message, boolean showToast) {
        Log.i(Const.TAG, message + "");
        if (showToast) {
            Toast.makeText(ApplicationLoader.applicationContext, message + "", Toast.LENGTH_LONG).show();
        }
    }

    public static void toast(String message) {
            Toast.makeText(ApplicationLoader.applicationContext, message + "", Toast.LENGTH_LONG).show();
    }

}
