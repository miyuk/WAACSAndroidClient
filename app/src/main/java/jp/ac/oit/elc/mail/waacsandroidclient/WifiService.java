package jp.ac.oit.elc.mail.waacsandroidclient;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by e1611100 on 2016/07/23.
 */
public class WifiService extends Service{
    private static final String TAG = WifiService.class.getSimpleName();
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig;
    private ServiceBinder mBinder;
    private int mLastStatus;
    private StatusChangedListener mStatusChangedListener;

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    private BroadcastReceiver mStatusChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            Log.d(TAG, String.format("Wi-Fi情報更新: %s", info.toString()));
            if(mWifiConfig == null || mWifiConfig.SSID != info.getSSID()){
                return;
            }
            SupplicantState state = (SupplicantState) intent.getExtras().get(WifiManager.EXTRA_NEW_STATE);
            int currentStatus = WifiStatus.fromSupplicantState(state);
            if(currentStatus != mLastStatus){
                mLastStatus = currentStatus;
                if(mStatusChangedListener != null){
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
        mBinder = new ServiceBinder();;
        mLastStatus = WifiStatus.UNKNOWN;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        unregisterReceiver(mStatusChangedReceiver);
        return super.onUnbind(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        IntentFilter filter = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(mStatusChangedReceiver, filter);
        return mBinder;
    }

    public void setWifiStatusChangedListener(StatusChangedListener listener){
        mStatusChangedListener = listener;
    }

    public boolean connectWifi(String ssid, String userId, String password) {
        WifiConfiguration config = new WifiConfiguration();
        WifiEnterpriseConfig eapConfig = new WifiEnterpriseConfig();
        config.SSID = "\"" + ssid + "\"";
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        eapConfig.setIdentity(userId);
        eapConfig.setPassword(password);
        eapConfig.setEapMethod(WifiEnterpriseConfig.Eap.TTLS);
        eapConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.PAP);
        config.enterpriseConfig = eapConfig;
        int networkId = mWifiManager.addNetwork(config);
        if (networkId < 0) {
            Log.e(TAG, "ネットワーク設定の追加失敗");
            return false;
        }
        mWifiManager.saveConfiguration();
        mWifiManager.updateNetwork(config);
        if (!mWifiManager.enableNetwork(networkId, true)) {
            Log.e(TAG, "ネットワーク設定の有効化失敗");
            return false;
        }
        mWifiConfig = config;
        return true;
    }

    public WifiConfiguration getWifiConfig() {
        return mWifiConfig;
    }

    public class ServiceBinder extends Binder {
        public ServiceBinder(){
        }
        public WifiService getService() {
            return WifiService.this;
        }
    }
    public interface StatusChangedListener{
        void onStatusChanged(WifiConfiguration config, int status);
    }
}
