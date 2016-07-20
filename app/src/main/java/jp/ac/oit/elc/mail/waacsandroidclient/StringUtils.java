package jp.ac.oit.elc.mail.waacsandroidclient;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yuuki on 2015/08/27.
 */
public class StringUtils {
    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static String quoteString(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        char firstChar = str.charAt(0);
        char lastChar = str.charAt(str.length() - 1);
        if (firstChar == '"' && lastChar == '"') {
            return str;
        }
        return "\"" + str + "\"";
    }

    public static Date parseDate(String str) {
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
        Date date;
        try {
            date = sdf.parse(str);
        } catch (Exception e) {
            return new Date();
        }
        return date;
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
        return sdf.format(date);
    }
}
