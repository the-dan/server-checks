package dan.serverchecks.ssl;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SavingTrustManager implements X509TrustManager {

	X509TrustManager original = null;

	X509Certificate[] serverCerts = null;
	CertificateException serverCheckResult = null;

	public X509Certificate[] getServerCerts() {
		return serverCerts;
	}

	public CertificateException getServerCheckResult() {
		return serverCheckResult;
	}

	public X509Certificate[] getClientCerts() {
		return clientCerts;
	}

	public CertificateException getClientCheckResult() {
		return clientCheckResult;
	}

	X509Certificate[] clientCerts = null;
	CertificateException clientCheckResult = null;

	public SavingTrustManager(X509TrustManager original) {
		this.original = original;
	}

	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		this.clientCerts = chain;
		try {
			original.checkClientTrusted(chain, authType);
		} catch (CertificateException e) {
			clientCheckResult = e;
			throw e;
		}
	}

	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
		this.serverCerts = chain;

		try {
			original.checkServerTrusted(chain, authType);
		} catch (CertificateException e) {
			serverCheckResult = e;
			throw e;
		}
	}

	public X509Certificate[] getAcceptedIssuers() {
		return original.getAcceptedIssuers();
	}
	
	public static SavingTrustManager[] getWrappedDefaultTrustManagers() {
		TrustManager[] tms = TrustManagerUtils.getDefaultTrustManagers();
		
		ArrayList<SavingTrustManager> usedTms = new ArrayList<SavingTrustManager>();
		
		for (TrustManager tm : tms) {
			if (tm instanceof X509TrustManager) {
				usedTms.add(new SavingTrustManager((X509TrustManager)tm));
			}
		}
		
		return usedTms.toArray(new SavingTrustManager[0]);
	}
	
	public static SavingTrustManager[] getWrappedTrustManagers(KeyStore ks) {
		
		TrustManager[] tms = TrustManagerUtils.getTrustManagersForKeystore(ks);
		
		ArrayList<SavingTrustManager> usedTms = new ArrayList<SavingTrustManager>();
		
		for (TrustManager tm : tms) {
			if (tm instanceof X509TrustManager) {
				usedTms.add(new SavingTrustManager((X509TrustManager)tm));
			}
		}
		
		return usedTms.toArray(new SavingTrustManager[0]);
	}

}