package telegram.messenger.xtelex.util;

import android.os.Environment;

import telegram.messenger.xtelex.ApplicationLoader;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

import sections.categories.DatabaseCategories;


public class Const {

    public static final String TAG = "const-tag";


    public static final int PRPMPTE_CODE = 46;
    public static final int TELEX_SETTINGS = 20;
    public static final int USER_CHANGE = 31;
    public static final int TEMP_CHATS = 58;
    public static final int ONLINE_CONTACT = 63;
    public static final int USER_FINDER = 65;
    public static final int FILE_MANAGER = 15;
    public static final int CATEGORY_MANAGEMENT = 14;
    public static final int TELEX_CHANNEL = 62;
    public static final int TELEX_SM_MANAGMENT = 68;
    public static final int THEME = 84;

    public static final String GHOST_MODE = "ghost_mode";
    public static final String MAINCONFIG = "mainconfig";

    public static final String PROXY_ENABLED = "proxy_enabled";
    public static final String PROXY_ANTY_FILTER_ENABLED = "proxy_anty_filter_enabled";
    public static final String SMART_PROXY_SETTINGS = "smartProxySettings";
    public static final String SMART_PROXY_CHANGE_TIME = "smart_proxy_change_time";

    public static final String CAT_DATABASE_NAME = "xtelex4";
    public static final String TEMP_CHAT_DATABASE_NAME = "xtelex9";
    public static final int CAT_DATABASE_VERSION = 10;
    public static final int TEMP_CHAT_DATABASE_VERSION = 10;

    public static final boolean PlayStoreVersion = true;
    public static final String BASE_URL_MANAGER = "";
    public static String DIR_DOWNLOAD_PATH = Environment.getExternalStorageDirectory() + "/Telegram/Apks/";

    public static boolean isPriview = false;

    public static int currentAccount = UserConfig.selectedAccount;



    public static ArrayList<TLRPC.TL_dialog> dialogsCats = new ArrayList<>();

    public static void setCats(int code) {
        DatabaseCategories catDB = new DatabaseCategories(ApplicationLoader.applicationContext);
        //catDB.open();
        dialogsCats.clear();
        int isCated;
        for (int i = 0; i < MessagesController.getInstance(currentAccount).dialogs.size(); i++) {
            TLRPC.TL_dialog d = MessagesController.getInstance(currentAccount).dialogs.get(i);
            isCated = catDB.isCategoried((int) d.id);
            if (isCated == code) {
                dialogsCats.add(d);
            }

        }

        catDB.close();
    }


    public static final String BASE_URL = "";





    public static final String PACKAGE = "telegram.messenger.xtelex";
    public static final String OFFICIALCHANNEL_URL = "";
    public static final String OFFICIALCHANNEL_URL_FA = "";
    public static final String OFFICIALCHANNEL_ID = "";
    public static final String SPAMBOT_URL = "";
    public static final String TELEX_CHANNEL_URL = "";



    public static final String ADMOB_APP_ID = "";

    public static final String CHAT_ACTIVITY_BANNER = "";
    public static final String GHOST_MODE_INTERSTITIAL = "";
    public static final String MENUES_INTERSTITIAL = "";
    public static final String REWARD_VIDEO = "";
    public static final String SECRETORYACTIVITY_BANNER = "";


    public static final String ACTIVE_AD = "active_ad";

    public static final String CATEGORY_MENU = "categoryMenu";









}
