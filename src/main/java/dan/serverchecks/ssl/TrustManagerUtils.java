package dan.serverchecks.ssl;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class TrustManagerUtils {

	// doesn't throw exceptions assuming that getting TrustManagerFactory with default algorithm and null keystore always works
	public static TrustManager [] getDefaultTrustManagers() {
		try {
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init((KeyStore)null);
			TrustManager[] tms = tmf.getTrustManagers();
			return tms;
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
}
