package jp.ac.oit.elc.mail.waacsandroidclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by e1611100 on 15/08/25.
 */
public class WifiConnector {
    private static final String TAG = WifiConnector.class.getSimpleName();
    private WifiManager mWifiManager;
    private Handler mHandler;
    private Context mContext;
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    private boolean isConnecting;
    private boolean isConnected;
    private String mSsid;
    private String mUserId;
    private String mPassword;


    public WifiConnector(Context context, final Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive");
                handleEvent(context, intent);
            }
        };
        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        context.registerReceiver(mReceiver, mFilter);
    }

    private void handleEvent(Context context, Intent intent) {
        WifiInfo info = mWifiManager.getConnectionInfo();
        String currentSsid = info.getSSID();
        String quotedSsid = StringUtils.quoteString(this.mSsid);
        Log.d(TAG, currentSsid);
        if (!(isConnecting || isConnected) || !quotedSsid.equals(currentSsid)) {
            return;
        }
        SupplicantState state = (SupplicantState) intent.getExtras().get(WifiManager.EXTRA_NEW_STATE);
        switch (state) {
            case COMPLETED:
            case GROUP_HANDSHAKE:
            case FOUR_WAY_HANDSHAKE:
            case ASSOCIATED:
                onConnected(state, info);
                break;
            case ASSOCIATING:
            case AUTHENTICATING:
            case SCANNING:
                onConnecting(state, info);
                break;
            case DORMANT:
            case DISCONNECTED:
            case INACTIVE:
            case INTERFACE_DISABLED:
            case INVALID:
            case UNINITIALIZED:
            default:
                onDisconnected(state, info);
                break;
        }

    }

    private void onConnected(SupplicantState state, WifiInfo info) {
        Message msg = new Message();
        msg.arg1 = WifiStatus.CONNECTED;
        msg.obj = state;
        mHandler.sendMessage(msg);
        isConnected = true;
        isConnecting = false;
    }

    private void onConnecting(SupplicantState state, WifiInfo info) {
        Message msg = new Message();
        msg.arg1 = WifiStatus.CONNECTING;
        msg.obj = state;
        mHandler.sendMessage(msg);
    }

    private void onDisconnected(SupplicantState state, WifiInfo info) {
        Message msg = new Message();
        msg.arg1 = WifiStatus.DISCONNECTED;
        msg.obj = state;
        mHandler.sendMessage(msg);
    }

    public void connect(String ssid, String userId, String password) {
        this.mSsid = ssid;
        this.mUserId = userId;
        this.mPassword = password;
        WifiConfiguration config = new WifiConfiguration();
        WifiEnterpriseConfig eapConfig = new WifiEnterpriseConfig();
        config.SSID = StringUtils.quoteString(ssid);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        eapConfig.setIdentity(userId);
        eapConfig.setPassword(password);
        eapConfig.setEapMethod(WifiEnterpriseConfig.Eap.TTLS);
        eapConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.PAP);
        config.enterpriseConfig = eapConfig;
        int networkId = mWifiManager.addNetwork(config);
        if (networkId < 0) {
            Log.e(TAG, "can't add NetworkConfig");
            return;
        }
        mWifiManager.saveConfiguration();
        mWifiManager.updateNetwork(config);
        for (WifiConfiguration item : mWifiManager.getConfiguredNetworks()) {
            mWifiManager.enableNetwork(config.networkId, false);
        }
        if (!mWifiManager.enableNetwork(networkId, true)) {
            Log.e(TAG, "can't enable Wifi");
            return;
        }
        Log.d(TAG, "接続完了");
        isConnecting = true;
    }


    public final static class WifiStatus {
        public final static int CONNECTED = 1;
        public final static int CONNECTING = 2;
        public final static int DISCONNECTED = -1;

    }
}
