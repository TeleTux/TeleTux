package tw.nekomimi.nekogram;

import android.content.SharedPreferences;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.NotificationCenter;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import tw.nekomimi.nekogram.database.NitritesKt;

public class NekoXConfig {

    //  public static String FAQ_URL = "https://telegra.ph/NekoX-FAQ-03-31";
    public static String FAQ_URL = "https://github.com/TeleTux/TeleTux#faq";
    public static int[] officialChats = {
            1223771542, // TeleTux Updates
            1199446479, // TeleTux Chat
            1199446479, // TeleTux Chat Channel
            1336955033, // TeleTux APKs
    };

    public static int[] developers = {
            1160156558, // freetux
            247133452
    };

    public static int[] maindevelopers = {
        1160156558 // freetux
};


    public static SharedPreferences preferences = NitritesKt.openMainSharedPreference("nekox_config");

    public static boolean developerModeEntrance;
    public static boolean developerMode = preferences.getBoolean("developer_mode", false);

    public static boolean disableFlagSecure = preferences.getBoolean("disable_flag_secure", false);
    public static boolean disableScreenshotDetection = preferences.getBoolean("disable_screenshot_detection", false);

    public static boolean disableStatusUpdate = preferences.getBoolean("disable_status_update", false);
    public static boolean hide_Me = preferences.getBoolean("hide_me", false);
    public static boolean keepOnlineStatus = preferences.getBoolean("keepOnlineStatus", false);

    public static int autoUpdateReleaseChannel = preferences.getInt("autoUpdateReleaseChannel", 2);
    public static String ignoredUpdateTag = preferences.getString("ignoredUpdateTag", "");
    public static long nextUpdateCheck = preferences.getLong("nextUpdateCheckTimestamp", 0);


    public static void toggleDeveloperMode() {

        preferences.edit().putBoolean("developer_mode", developerMode = !developerMode).apply();

        if (!developerMode) {

            preferences.edit()
                    .putBoolean("disable_flag_secure", disableFlagSecure = false)
                    .putBoolean("disable_screenshot_detection", disableScreenshotDetection = false)
                    .putBoolean("disable_status_update", disableStatusUpdate = false)
                    .putBoolean("hide_me", hide_Me = false)
                    .apply();


        }

    }

    public static void toggleDisableFlagSecure() {

        preferences.edit().putBoolean("disable_flag_secure", disableFlagSecure = !disableFlagSecure).apply();

    }

    public static void toggleDisableScreenshotDetection() {

        preferences.edit().putBoolean("disable_screenshot_detection", disableScreenshotDetection = !disableScreenshotDetection).apply();

    }

    public static int customApi = preferences.getInt("custom_api", 0);
    public static int customAppId = preferences.getInt("custom_app_id", 0);
    public static String customAppHash = preferences.getString("custom_app_hash", "");

    public static int currentAppId() {

        switch (customApi) {

            case 0:
                return BuildConfig.APP_ID;
            case 1:
                return BuildVars.OFFICAL_APP_ID;
            case 2:
                return BuildVars.TGX_APP_ID;
            default:
                return customAppId;

        }

    }

    public static String currentAppHash() {

        switch (customApi) {

            case 0:
                return BuildConfig.APP_HASH;
            case 1:
                return BuildVars.OFFICAL_APP_HASH;
            case 2:
                return BuildVars.TGX_APP_HASH;
            default:
                return customAppHash;

        }

    }

    public static void saveCustomApi() {

        preferences.edit()
                .putInt("custom_api", customApi)
                .putInt("custom_app_id", customAppId)
                .putString("custom_app_hash", customAppHash)
                .apply();

    }

    public static String customDcIpv4 = preferences.getString("custom_dc_v4", "");
    public static String customDcIpv6 = preferences.getString("custom_dc_v6", "");
    public static int customDcPort = preferences.getInt("custom_dc_port", 0);
    public static int customDcLayer = preferences.getInt("custom_dc_layer", 0);

    public static String customDcPublicKey = preferences.getString("custom_dc_public_key", "");
    public static long customDcFingerprint = preferences.getLong("custom_dc_fingerprint", 0L);

    public static void saveCustomDc() {

        preferences.edit()
                .putString("custom_dc_v4", customDcIpv4)
                .putString("custom_dc_v6", customDcIpv6)
                .putInt("custom_dc_port", customDcPort)
                .putInt("custom_dc_layer", customDcLayer)
                .putString("custom_dc_public_key", customDcPublicKey)
                .putLong("custom_dc_fingerprint", customDcFingerprint)
                .apply();

    }

    public static void toggleDisableStatusUpdate() {

        preferences.edit().putBoolean("disable_status_update", disableStatusUpdate = !disableStatusUpdate).apply();

        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateUserStatus, (Object) null);

    }

    public static void toggleKeepOnlineStatus() {

        preferences.edit().putBoolean("keepOnlineStatus", keepOnlineStatus = !keepOnlineStatus).apply();

        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.updateUserStatus, (Object) null);

    }

    public static void togglehide_Me() {

        preferences.edit().putBoolean("hide_Me", hide_Me = !hide_Me).apply();

    }

    public static void setAutoUpdateReleaseChannel(int channel) {
        preferences.edit().putInt("autoUpdateReleaseChannel", autoUpdateReleaseChannel = channel).apply();
    }

    public static void setIgnoredUpdateTag(String ignored) {
        preferences.edit().putString("ignoredUpdateTag", ignoredUpdateTag = ignored).apply();
    }

    public static void setNextUpdateCheck(long timestamp) {
        preferences.edit().putLong("nextUpdateCheckTimestamp",  nextUpdateCheck = timestamp).apply();
    }

}