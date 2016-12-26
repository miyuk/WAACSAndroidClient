package jp.ac.oit.elc.mail.waacsandroidclient;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by e1611100 on 2016/10/30.
 */

public class AsyncWebApiClient extends AsyncTask<URL, Void, String> {

    private OnGetListener mOnGetListener;

    @Override
    protected void onPostExecute(String s) {
        if (mOnGetListener != null) {
            mOnGetListener.onGet(s);
        }
    }

    @Override
    protected String doInBackground(URL... urls) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) urls[0].openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("not http ok");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            reader.close();
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;

    }

    public void setOnGetListener(OnGetListener listener) {
        mOnGetListener = listener;
    }

    public interface OnGetListener {
        void onGet(String body);
    }
}

