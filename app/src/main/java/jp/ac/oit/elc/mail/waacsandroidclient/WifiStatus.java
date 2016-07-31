package jp.ac.oit.elc.mail.waacsandroidclient;

import android.net.wifi.SupplicantState;

/**
 * Created by e1611100 on 2016/07/27.
 */
public final class WifiStatus {
    public static int fromSupplicantState(SupplicantState supplicantState) {
        switch (supplicantState) {
            case COMPLETED:
            case GROUP_HANDSHAKE:
            case FOUR_WAY_HANDSHAKE:
            case ASSOCIATED:
                return CONNECTED;
            case ASSOCIATING:
            case AUTHENTICATING:
            case SCANNING:
                return CONNECTING;
            case DORMANT:
            case DISCONNECTED:
            case INACTIVE:
            case INTERFACE_DISABLED:
            case INVALID:
            case UNINITIALIZED:
                return DISCONNECTED;
            default:
                return UNKNOWN;
        }
    }

    public final static int CONNECTED = 1;
    public final static int CONNECTING = 2;
    public final static int DISCONNECTED = -1;
    public final static int UNKNOWN = -2;
}