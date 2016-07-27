package jp.ac.oit.elc.mail.waacsandroidclient;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
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
//    private Handler mUiHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            int status = msg.arg1;
//            String authState = msg.obj.toString();
//            switch (status) {
//                case WifiConnector.WifiStatus.CONNECTED:
//                    writeLog("Wi-Fi接続完了: " + authState);
//
//                    imageStatus.setImageResource(R.drawable.connected);
//                    break;
//                case WifiConnector.WifiStatus.CONNECTING:
//                    writeLog("Wi-Fi接続中: " + authState);
//                    imageStatus.setImageResource(R.drawable.connecting);
//                    break;
//                case WifiConnector.WifiStatus.DISCONNECTED:
//                    writeLog("Wi-Fi切断: " + authState);
//                    imageStatus.setImageResource(R.drawable.unconnected);
//                    break;
//                default:
//                    break;
//            }
//        }
//    };

    private ServiceConnection mWifiServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, String.format("サービス接続：%s", componentName.getClassName()));
            WifiService.WifiServiceBinder binder = (WifiService.WifiServiceBinder) iBinder;
            mWifiService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, String.format("サービス切断：%s", componentName.getClassName()));
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
        super.onStop();
        unbindService(mWifiServiceConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        assignViews();

        String[] permissions = getRequiredPermissions();
        List<String> requestedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestedPermissions.add(permission);
            }
        }
        if (requestedPermissions.size() == 0) {
            mHasRequiredPermissions = true;
        } else {
            ActivityCompat.requestPermissions(this, (String[]) requestedPermissions.toArray(), 0);
        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!mNfcAdapter.isEnabled()) {
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }
        if (!mNfcAdapter.isNdefPushEnabled()) {
            startActivity(new Intent(Settings.ACTION_NFCSHARING_SETTINGS));
        }
        bindService(new Intent(this, WifiService.class), mWifiServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //intent内のNDEFメッセージを取得後、次のintentのためにnullにする
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        setIntent(null);
        String msg = getWaacsMessage(intent);
        if (msg == null) {
            return;
        }
        //payloadをパラメータに変換
        Parameter param = Parameter.parse(msg);
        //受け取ったパラメータを画面表示
        displayParameter(param);
        //パラメータを使ってWifi接続
        writeLog("Wi-Fi接続処理開始");
        Log.d(TAG, "接続開始");
        if (mWifiService != null) {
            mWifiService.connectWifi(param.ssid, param.userId, param.password);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Log.i(TAG, "receive NDEF intent");
            setIntent(intent);
        } else {
            Log.w(TAG, "can't understand receiving intent");
            setIntent(null);
        }
    }

    private String getWaacsMessage(Intent intent) {
        if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            return null;
        }
        writeLog("NFCメッセージ受信");
        //IntentからNDEFメッセージを取り出し
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage ndefMessage = (NdefMessage) rawMessages[0];
        NdefRecord topRecord = ndefMessage.getRecords()[0];
        //NDEF Recordがext:waacs:msgでなければreturn null
        if (topRecord.getTnf() != NdefRecord.TNF_EXTERNAL_TYPE || !WAACS_MESSAGE_RECORD_TYPE.equals(new String(topRecord.getType()))) {
            Log.e(TAG, "empty waacs message");
            return null;
        }
        String payload = "";
        for (NdefRecord record : ndefMessage.getRecords()) {
            payload += new String(record.getPayload());
        }
        return payload;
    }

    private void displayParameter(Parameter parameter) {
        textSsid.setText(parameter.ssid);
        textUserId.setText(parameter.userId);
        textPassword.setText(parameter.password);
        textIssuanceTime.setText(StringUtils.formatDate(parameter.issuanceTime));
        textExpirationTime.setText(StringUtils.formatDate(parameter.expirationTime));
    }

}
