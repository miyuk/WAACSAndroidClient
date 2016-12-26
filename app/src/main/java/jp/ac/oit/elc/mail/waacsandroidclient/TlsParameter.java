package jp.ac.oit.elc.mail.waacsandroidclient;

import android.os.MemoryFile;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by yuuki on 2016/11/13.
 */

public class TlsParameter {
    public String clientCertificateName;
    public X509Certificate clientCertificate;
    public PrivateKey clientPrivateKey;
    public String passphrase;

    public static TlsParameter parse(JSONObject tlsObj) throws JSONException, KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        TlsParameter param = new TlsParameter();
        param.clientCertificateName = tlsObj.getString(Attribute.ClientCertificateName);
        param.passphrase = tlsObj.getString(Attribute.Passphrase);

        String contentBase64 = tlsObj.getString(Attribute.ClientCertificateContent);
        byte[] content = Base64.decode(contentBase64, Base64.DEFAULT);
        MemoryFile mf = new MemoryFile(param.clientCertificateName, content.length);
        try (OutputStream output = mf.getOutputStream()) {
            output.write(content);
            output.flush();
        }
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream input = mf.getInputStream()) {
            ks.load(input, param.passphrase.toCharArray());
            String alias = ks.aliases().nextElement();
            param.clientCertificate = (X509Certificate) ks.getCertificate(alias);
            param.clientPrivateKey = (PrivateKey) ks.getKey(alias, param.passphrase.toCharArray());
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }
        mf.close();
        return param;
    }

    public static final class Attribute {
        public static final String ClientCertificateName = "clientCertificateName";
        public static final String ClientCertificateContent = "clientCertificateContent";
        public static final String Passphrase = "passphrase";
    }
}
