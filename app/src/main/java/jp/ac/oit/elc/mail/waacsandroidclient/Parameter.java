package jp.ac.oit.elc.mail.waacsandroidclient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

/**
 * Created by e1611100 on 15/08/25.
 */
public class Parameter {
    public static final String TYPE_TLS = "EAP-TLS";
    public static final String TYPE_TTLS = "EAP-TTLS";

    public String ssid;
    public String eapType;
    public Date issuanceTime;
    public Date expirationTime;
    public TlsParameter tlsParameter;
    public TtlsParameter ttlsParameter;

    public Parameter() {
        ssid = null;
        eapType = null;
        issuanceTime = null;
        expirationTime = null;
        tlsParameter = null;
        ttlsParameter = null;
    }

    public static Parameter parse(JSONObject json) throws JSONException, IOException {
        Parameter param = new Parameter();
        param.ssid = json.getString(Attribute.SSID);
        param.eapType = json.getString(Attribute.EAP_TYPE);
        if(param.eapType.equals(TYPE_TLS)){
            JSONObject tls = json.getJSONObject(Attribute.TLS_PARAMETER);
            try{
                param.tlsParameter = TlsParameter.parse(tls);
            }catch(Exception e){
                e.printStackTrace();
                throw new IOException(e.getMessage());
            }
        }
        if(param.eapType.equals(TYPE_TTLS)){
            JSONObject ttls = json.getJSONObject(Attribute.TTLS_PARAMETER);
            param.ttlsParameter = TtlsParameter.parse(ttls);
        }
        if(json.has(Attribute.ISSUANCE_TIME)){
            param.issuanceTime = StringUtils.parseDate(json.getString(Attribute.ISSUANCE_TIME));
        }
        if(json.has(Attribute.EXPIRATION_TIME)) {
            param.expirationTime = StringUtils.parseDate(json.getString(Attribute.EXPIRATION_TIME));
        }
        return param;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("ssid: %s eapType: %s\n", ssid, eapType));
        if(eapType.equals(TYPE_TLS)){
            builder.append(String.format("tls parameter: name: %s common name: %s key: %s passphrase: %s", tlsParameter.clientCertificateName,
                    tlsParameter.clientCertificate.getSubjectDN().toString(), tlsParameter.clientPrivateKey.getFormat(), tlsParameter.passphrase));
        }
        else if(eapType.equals(TYPE_TTLS)){
            builder.append(String.format("ttls parameter: userId: %s password: %s", ttlsParameter.userId, ttlsParameter.password));
        }
        return builder.toString();
    }

    public static final class Attribute {
        public static final String SSID = "ssid";
        public static final String EAP_TYPE = "eapType";
        public static final String TLS_PARAMETER = "tlsParameter";
        public static final String TTLS_PARAMETER = "ttlsParameter";

        public static final String ISSUANCE_TIME = "issuanceTime";
        public static final String EXPIRATION_TIME = "expirationTime";
    }


}
