package jp.ac.oit.elc.mail.waacsandroidclient;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by e1611100 on 2016/07/23.
 */
public class WifiService extends Service {
    private static final String TAG = WifiService.class.getSimpleName();
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig;
    private ServiceBinder mBinder;
    private int mLastStatus;
    private StatusChangedListener mStatusChangedListener;
    private BroadcastReceiver mStatusChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            Log.d(TAG, String.format("Wi-Fi情報更新: %s", info.toString()));
            if (mWifiConfig == null || !mWifiConfig.SSID.equals(info.getSSID())) {
                return;
            }
            SupplicantState state = (SupplicantState) intent.getExtras().get(WifiManager.EXTRA_NEW_STATE);
            int currentStatus = WifiStatus.fromSupplicantState(state);
            if (currentStatus != mLastStatus) {
                mLastStatus = currentStatus;
                if (mStatusChangedListener != null) {
                    mStatusChangedListener.onStatusChanged(mWifiConfig, currentStatus);
                }
            }
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mStatusChangedListener = null;
        mWifiConfig = null;
        mBinder = new ServiceBinder();
        ;
        mLastStatus = WifiStatus.UNKNOWN;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        IntentFilter filter = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(mStatusChangedReceiver, filter);
        return mBinder;
    }

    public void setWifiStatusChangedListener(StatusChangedListener listener) {
        mStatusChangedListener = listener;
    }

    public boolean connectWifi(Parameter param){
        WifiConfiguration config = parseWifiConfiguration(param);
        int networkId = mWifiManager.addNetwork(config);
        if (networkId < 0) {
            Log.e(TAG, "ネットワーク設定の追加失敗");
            return false;
        }
        if (!mWifiManager.saveConfiguration() || !mWifiManager.enableNetwork(networkId, true) || !mWifiManager.reconnect()) {
            Log.e(TAG, "ネットワーク設定の有効化失敗");
            return false;
        }
        mWifiConfig = config;
        return true;
    }
    public static WifiConfiguration parseWifiConfiguration(Parameter param){
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + param.ssid + "\"";
        config.status = WifiConfiguration.Status.ENABLED;
        config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
        config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        WifiEnterpriseConfig eapConfig = new WifiEnterpriseConfig();
        if(param.eapType.equals(Parameter.TYPE_TLS)){
            eapConfig.setEapMethod(WifiEnterpriseConfig.Eap.TLS);
            eapConfig.setClientKeyEntry(param.tlsParameter.clientPrivateKey, param.tlsParameter.clientCertificate);
            eapConfig.setIdentity(param.tlsParameter.clientCertificateName);
        }else if(param.eapType.equals(Parameter.TYPE_TTLS)){
            eapConfig.setEapMethod(WifiEnterpriseConfig.Eap.TTLS);
            eapConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.PAP);
            eapConfig.setIdentity(param.ttlsParameter.userId);
            eapConfig.setPassword(param.ttlsParameter.password);
        }
        config.enterpriseConfig = eapConfig;
        return config;
    }
    public WifiConfiguration getWifiConfig() {
        return mWifiConfig;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        unregisterReceiver(mStatusChangedReceiver);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    public class ServiceBinder extends Binder {
        public ServiceBinder() {
        }

        public WifiService getService() {
            return WifiService.this;
        }
    }

    public interface StatusChangedListener {
        void onStatusChanged(WifiConfiguration config, int status);

    }
}
