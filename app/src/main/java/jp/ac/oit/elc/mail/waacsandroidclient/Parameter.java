package jp.ac.oit.elc.mail.waacsandroidclient;

import android.util.JsonReader;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;

/**
 * Created by e1611100 on 15/08/25.
 */
public class Parameter {
    public String ssid;
    public String userId;
    public String password;
    public Date issuanceTime;
    public Date expirationTime;

    public Parameter() {
        ssid = null;
        userId = null;
        password = null;
        issuanceTime = null;
        expirationTime = null;
    }

    public static Parameter parse(String jsonText) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(jsonText));
        Parameter parameter = new Parameter();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (Attribute.SSID.equals(name)) {
                parameter.ssid = reader.nextString();
            } else if (Attribute.USER_ID.equals(name)) {
                parameter.userId = reader.nextString();
            } else if (Attribute.PASSWORD.equals(name)) {
                parameter.password = reader.nextString();
            } else if (Attribute.ISSUANCE_TIME.equals(name)) {
                parameter.issuanceTime = StringUtils.parseDate(reader.nextString());
            } else if (Attribute.EXPIRATION_TIME.equals(name)) {
                parameter.expirationTime = StringUtils.parseDate(reader.nextString());
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return parameter;
    }

    public String toJson() {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(stringWriter);
        try {
            jsonWriter.beginObject();
            if (ssid != null) {
                jsonWriter.name(Attribute.SSID).value(ssid);
            }
            if (userId != null) {
                jsonWriter.name(Attribute.USER_ID).value(userId);
            }
            if (password != null) {
                jsonWriter.name(Attribute.PASSWORD).value(password);
            }
            if (issuanceTime != null) {
                jsonWriter.name(Attribute.ISSUANCE_TIME).value(StringUtils.formatDate(issuanceTime));
            }
            if (expirationTime != null) {
                jsonWriter.name(Attribute.EXPIRATION_TIME).value(StringUtils.formatDate(expirationTime));
            }
            jsonWriter.endObject();
            jsonWriter.close();
            return stringWriter.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return String.format("ssid: %s, user_id: %s, password: %s, issuance_time: %s, expiration_time: %s", ssid, userId, password, issuanceTime.toString(), expirationTime.toString());
    }

    public static final class Attribute {
        public static final String SSID = "ssid";
        public static final String USER_ID = "userId";
        public static final String PASSWORD = "password";
        public static final String ISSUANCE_TIME = "issuanceTime";
        public static final String EXPIRATION_TIME = "expirationTime";
    }
}
