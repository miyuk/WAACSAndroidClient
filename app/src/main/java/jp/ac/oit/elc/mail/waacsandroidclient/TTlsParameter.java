package jp.ac.oit.elc.mail.waacsandroidclient;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yuuki on 2016/11/13.
 */

public class TtlsParameter {
    public String userId;
    public String password;
    public static TtlsParameter parse(JSONObject ttlsObj) throws JSONException{
        TtlsParameter param = new TtlsParameter();
        param.userId = ttlsObj.getString(Attribute.USER_ID);
        param.password = ttlsObj.getString(Attribute.PASSWORD);
        return param;
    }

    public static final class Attribute{
        public static final String USER_ID = "userId";
        public static final String PASSWORD = "password";
    }
}
