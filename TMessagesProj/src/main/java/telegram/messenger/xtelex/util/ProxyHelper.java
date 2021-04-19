package telegram.messenger.xtelex.util;

import android.os.SystemClock;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestTimeDelegate;

import telegram.messenger.xtelex.R;

public class ProxyHelper {

    private int currentConnectionState;


    public static void checkPing(SharedConfig.ProxyInfo proxyInfo, CallbackPing callbackPing) {
        proxyInfo.proxyCheckPingId = ConnectionsManager.getInstance(UserConfig.selectedAccount).checkProxy(proxyInfo.address, proxyInfo.port, proxyInfo.username, proxyInfo.password, proxyInfo.secret, new RequestTimeDelegate() {
            @Override
            public void run(final long time) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        proxyInfo.availableCheckTime = SystemClock.elapsedRealtime();
                        proxyInfo.checking = false;
                        if (time == -1) {
                            proxyInfo.available = false;
                            proxyInfo.ping = 0;
                            callbackPing.ping(proxyInfo.ping);
                        } else {
                            proxyInfo.ping = time;
                            proxyInfo.available = true;
                            callbackPing.ping(proxyInfo.ping);
                        }

                    }
                });
            }
        });
    }

    public String updateStatus(SharedConfig.ProxyInfo proxyInfo) {
        if (proxyInfo.checking) {
            return LocaleController.getString("Checking", R.string.Checking);

        } else if (proxyInfo.available) {
            if (proxyInfo.ping != 0) {
                return LocaleController.getString("Available", R.string.Available) + ", " + LocaleController.formatString("Ping", R.string.Ping, proxyInfo.ping);
            } else {
                return LocaleController.getString("Available", R.string.Available);
            }

        } else {
            return LocaleController.getString("Unavailable", R.string.Unavailable);

        }

    }

    public static String updateStatus(long ping) {
        if (ping != 0) {
            return LocaleController.getString("Available", R.string.Available) + ", " + LocaleController.formatString("Ping", R.string.Ping, ping);
        } else {
            return LocaleController.getString("Unavailable", R.string.Unavailable);
        }

    }


    public interface CallbackPing {
        void ping(long ping);
    }
}
