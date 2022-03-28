package tw.nekomimi.nekogram;


import android.content.Context;
import android.graphics.Typeface;

/*
    Each call to Typeface.createFromAsset will load a new instance of the typeface into memory,
    and this memory is not consistently get garbage collected
    http://code.google.com/p/android/issues/detail?id=9904
    (It states released but even on Lollipop you can see the typefaces accumulate even after
    multiple GC passes)
    You can detect this by running:
    adb shell dumpsys meminfo com.your.packagenage
    You will see output like:
     Asset Allocations
        zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Medium.ttf: 125K
        zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Medium.ttf: 125K
        zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Medium.ttf: 125K
        zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Regular.ttf: 123K
        zip:/data/app/com.your.packagenage-1.apk:/assets/Roboto-Medium.ttf: 125K
*/
public class MeowTypefaceHelper {

    private static Typeface regularFont;
    private static Typeface mediumFont;
    private static Typeface boldFont;
    private static Typeface lightFont;
    private static int fontPadding;

    public static void init(Typeface regular, Typeface medium, Typeface bold, Typeface light, int fonPaddingDP) {
        regularFont = regular;
        mediumFont = medium;
        boldFont = bold;
        lightFont = light;
        fontPadding = fonPaddingDP;
    }

    public static void init(Typeface regular, Typeface medium, Typeface bold, Typeface light) {
        regularFont = regular;
        mediumFont = medium;
        boldFont = bold;
        lightFont = light;
        fontPadding = 0;
    }

    public static Typeface getRegularFont() {
        if(regularFont == null)
            return Typeface.DEFAULT;
        return regularFont;
    }

    public static Typeface getMediumFont() {
        if(mediumFont == null)
            return Typeface.DEFAULT;
        return mediumFont;
    }

    public static Typeface getBoldFont() {
        if(boldFont == null)
            return Typeface.DEFAULT;
        return boldFont;
    }

    public static Typeface getLightFont() {
        if(lightFont == null)
            return Typeface.DEFAULT;
        return lightFont;
    }

    public static int getFontPadding() {
        return fontPadding;
    }
}
