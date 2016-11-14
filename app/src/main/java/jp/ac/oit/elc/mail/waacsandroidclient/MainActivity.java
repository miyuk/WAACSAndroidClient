package jp.ac.oit.elc.mail.waacsandroidclient;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, WifiService.StatusChangedListener, AsyncWebApiClient.OnGetListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String WAACS_MESSAGE_RECORD_TYPE = "waacs:msg";
    private NfcAdapter mNfcAdapter;
    private WifiService mWifiService;
    private ServiceConnection mConnection;
    private TextView textSsid;
    private TextView textEapType;
    private TextView textLog;
    private ImageView imageStatus;
    private Button buttonQrScan;
    private Button buttonEnquete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assignViews();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (!mNfcAdapter.isEnabled()) {
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        }
    }

    private void assignViews() {
        imageStatus = (ImageView) findViewById(R.id.imageStatus);
        textSsid = (TextView) findViewById(R.id.textSsid);
        textEapType = (TextView) findViewById(R.id.textEapType);
        textLog = (TextView) findViewById(R.id.textLog);
        buttonQrScan = (Button) findViewById(R.id.buttonQrScan);
        buttonQrScan.setOnClickListener(this);
        buttonEnquete = (Button) findViewById(R.id.buttonEnquete);
        buttonEnquete.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        //intent内のNDEFメッセージを取得後、次のintentのためにnullにする
        Intent intent = getIntent();
        setIntent(null);
        if (intent != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Log.i(TAG, String.format("NDEFメッセージ受信: %s", intent.toString()));
            writeLog("NFCメッセージ受信");
            //IntentからNDEFメッセージを取り出し
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage ndefMessage = (NdefMessage) rawMessages[0];
            URL url = null;
            try {
                url = parseWaacsMessage(ndefMessage);
                requestWifiAuth(url);
            } catch (Exception e) {
                e.printStackTrace();
                writeLog("NFC受信エラー");
                return;
            }
        }
    }

    //NFCはすべてonResume()で処理
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
        Log.d(TAG, builder.toString());
    }

    private void requestWifiAuth(URL url) {
        AsyncWebApiClient client = new AsyncWebApiClient();
        client.setOnGetListener(this);
        client.execute(url);
        writeLog(String.format("Wi-Fi認証情報要求 URL: %s".format(url.toString())));
    }

    private void connectWifi(final Parameter param) {
        if (mConnection != null && mWifiService != null) {
            mWifiService.connectWifi(param);
        } else {
            mConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    Log.d(TAG, String.format("サービス接続: %s", componentName.getShortClassName()));
                    WifiService.ServiceBinder binder = (WifiService.ServiceBinder) iBinder;
                    mWifiService = binder.getService();
                    mWifiService.setWifiStatusChangedListener(MainActivity.this);
                    mWifiService.connectWifi(param);
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    Log.d(TAG, String.format("サービス異常切断: %s", componentName.getShortClassName()));
                    mWifiService = null;
                }
            };
            Log.d(TAG, "サービス初回作成");
            bindService(new Intent(this, WifiService.class), mConnection, BIND_AUTO_CREATE);
        }
    }

    private URL parseWaacsMessage(NdefMessage ndefMessage) throws Exception {
        //NDEF Messageがext:waacs:msgか確認
        for (NdefRecord record : ndefMessage.getRecords()) {
            if (record.getTnf() == NdefRecord.TNF_EXTERNAL_TYPE && WAACS_MESSAGE_RECORD_TYPE.equals(new String(record.getType()))) {
                String jsonText = new String(record.getPayload());
                return new URL(jsonText);
            }
        }
        throw new Exception("RecordTypeの不一致");
    }

    private void displayParameter(Parameter parameter) {
        textSsid.setText(parameter.ssid);
        textEapType.setText(parameter.eapType);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonQrScan:
                IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setPrompt("QRコードを読み込んでください");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(true);
                integrator.setBarcodeImageEnabled(true);
                integrator.setOrientationLocked(false);
                integrator.initiateScan();
                break;
            case R.id.buttonEnquete:
                Uri uri = Uri.parse("https://goo.gl/forms/OzrJedTWgVxwUi3g1");
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(i);
                break;
        }
    }

    @Override
    public void onStatusChanged(WifiConfiguration config, int status) {
        if (status == WifiStatus.CONNECTED) {
            writeLog(String.format("Wi-Fi接続完了: %s", config.SSID));
            imageStatus.setImageResource(R.drawable.connected);
        } else if (status == WifiStatus.CONNECTING) {
            writeLog(String.format("Wi-Fi接続中: %s", config.SSID));
            imageStatus.setImageResource(R.drawable.connecting);
        } else {
            writeLog(String.format("Wi-Fi切断: %s", config.SSID));
            imageStatus.setImageResource(R.drawable.disconnected);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result == null || result.getContents() == null) {
            Toast.makeText(this, "QR読み取りキャンセル", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            URL url = new URL(result.getContents());
            requestWifiAuth(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mConnection != null) {
            unbindService(mConnection);
            mConnection = null;
            mWifiService = null;
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onGet(String body) {
        try {
            if(body == null){
                Toast.makeText(this, "サーバに接続できませんでした", Toast.LENGTH_LONG).show();
                writeLog(String.format("ネットワークエラー"));
                return;
            }
            JSONObject json = new JSONObject(body);
            Parameter param = Parameter.parse(json);
            writeLog(String.format("Wi-Fi認証情報取得 SSID: %s EAP-TYPE: %s", param.ssid, param.eapType));
            displayParameter(param);
            connectWifi(param);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }
}
