package jp.ac.oit.elc.mail.waacsandroidclient;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String WAACS_MESSAGE_RECORD_TYPE = "waacs:msg";
    private NfcAdapter mNfcAdapter;
    private ImageView imageStatus;
    private WifiService mWifiService;
    private boolean mHasRequiredPermissions;
    private TextView textSsid;
    private TextView textUserId;
    private TextView textPassword;
    private TextView textIssuanceTime;
    private TextView textExpirationTime;
    private TextView textLog;

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }




    private WifiService.StatusChangedListener mWifiStatusChangedListener = new WifiService.StatusChangedListener() {
        @Override
        public void onStatusChanged(WifiConfiguration config, int status) {
            if(status == WifiStatus.CONNECTED){
                writeLog(String.format(Locale.JAPAN, "Wi-Fi接続完了: %d", status));
                imageStatus.setImageResource(R.drawable.connected);
            }else if(status == WifiStatus.CONNECTING){
                writeLog(String.format("Wi-Fi接続中: %d", status));
                imageStatus.setImageResource(R.drawable.connecting);
            }else{
                writeLog(String.format("Wi-Fi切断: %d", status));
                imageStatus.setImageResource(R.drawable.disconnected);
            }
        }
    };

    private ServiceConnection mWifiServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, String.format("サービス接続: %s", componentName.getShortClassName()));
            WifiService.ServiceBinder binder = (WifiService.ServiceBinder) iBinder;
            mWifiService = binder.getService();
            mWifiService.setWifiStatusChangedListener(mWifiStatusChangedListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, String.format("サービス異常切断: %s", componentName.getShortClassName()));
            mWifiService = null;
        }
    };

    private void assignViews() {
        imageStatus = (ImageView) findViewById(R.id.imageStatus);
        textSsid = (TextView) findViewById(R.id.textSsid);
        textUserId = (TextView) findViewById(R.id.textUserId);
        textPassword = (TextView) findViewById(R.id.textPassword);
        textIssuanceTime = (TextView) findViewById(R.id.textRegistTime);
        textExpirationTime = (TextView) findViewById(R.id.textExpireTime);
        textLog = (TextView) findViewById(R.id.textLog);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PermissionChecker.PERMISSION_GRANTED)
                Log.e(TAG, String.format("パーミッション取得失敗：%s", permissions[i]));
        }
    }

    private String[] getRequiredPermissions() {
        PackageManager packageManager = getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return packageInfo.requestedPermissions;
    }

    private boolean alreadyHasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void writeLog(String message) {
        StringBuilder builder = new StringBuilder();
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        builder.append(textLog.getText());
        builder.append(sdf.format(now));
        builder.append(": ");
        builder.append(message);
        builder.append("\n");
        textLog.setText(builder.toString());
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignViews();
//        String[] permissions = getRequiredPermissions();
//        List<String> requestedPermissions = new ArrayList<>();
//        for (String permission : permissions) {
//            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//                requestedPermissions.add(permission);
//            }
//        }
//        if (requestedPermissions.size() == 0) {
//            mHasRequiredPermissions = true;
//        } else {
//            ActivityCompat.requestPermissions(this, (String[]) requestedPermissions.toArray(), 0);
//        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!mNfcAdapter.isEnabled()) {
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mWifiServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, WifiService.class), mWifiServiceConnection, BIND_AUTO_CREATE);
        //intent内のNDEFメッセージを取得後、次のintentのためにnullにする
        Intent intent = getIntent();
        setIntent(null);
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Log.i(TAG, String.format("NDEFメッセージ受信: %s", intent.toString()));
            writeLog("NFCメッセージ受信");
            //IntentからNDEFメッセージを取り出し
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage ndefMessage = (NdefMessage) rawMessages[0];
            Parameter param = null;
            try{
                param = parseWaacsMessage(ndefMessage);
            }catch (Exception e){
                e.printStackTrace();
                writeLog("NFC受信エラー");
                return;
            }
            //受け取ったパラメータを画面表示
            displayParameter(param);
            //パラメータを使ってWifi接続
            if (mWifiService != null) {
                writeLog("Wi-Fi接続処理開始");
                mWifiService.connectWifi(param.ssid, param.userId, param.password);
            }
        }
    }
    private Parameter parseWaacsMessage(NdefMessage ndefMessage) throws Exception{
        NdefRecord topRecord = ndefMessage.getRecords()[0];
        //NDEF Messageがext:waacs:msgか確認
        if (topRecord.getTnf() != NdefRecord.TNF_EXTERNAL_TYPE || !WAACS_MESSAGE_RECORD_TYPE.equals(new String(topRecord.getType()))) {
            throw new Exception("RecordTypeの不一致");
        }
        String jsonText = getNdefPayload(ndefMessage);
        //payloadをパラメータに変換
        Parameter param = Parameter.parse(jsonText);
        return param;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private String getNdefPayload(NdefMessage ndefMessage) {
        StringBuilder builder = new StringBuilder();
        for (NdefRecord record : ndefMessage.getRecords()) {
            builder.append(new String(record.getPayload()));
        }
        return builder.toString();
    }

    private void displayParameter(Parameter parameter) {
        textSsid.setText(parameter.ssid);
        textUserId.setText(parameter.userId);
        textPassword.setText(parameter.password);
        textIssuanceTime.setText(StringUtils.formatDate(parameter.issuanceTime));
        textExpirationTime.setText(StringUtils.formatDate(parameter.expirationTime));
    }

}
