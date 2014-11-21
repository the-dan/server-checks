package dan.serverchecks.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.Certificate;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.openssl.PEMReader;

import ch.qos.logback.core.net.ssl.KeyStoreFactoryBean;

public class TrustManagerUtils {

	// doesn't throw exceptions assuming that getting TrustManagerFactory with
	// default algorithm and null keystore always works
	public static TrustManager[] getDefaultTrustManagers() {
		return getTrustManagersForKeystore(null);
	}

	public static TrustManager[] getTrustManagersForKeystore(KeyStore ks) {
		try {
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ks);
			TrustManager[] tms = tmf.getTrustManagers();
			return tms;
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static KeyStore openKeystoreFromStream(String keystoreType,
			InputStream keystoreData, String password)
			throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException {
		KeyStore ks = KeyStore.getInstance(keystoreType);

		try {
			ks.load(keystoreData, password.toCharArray());
		} catch (IOException e) {
			System.err.println("Error loading keystore: " + e.getMessage());
			return null;
		}

		return ks;
	}

	public static KeyStore tryOpeningKeystoreFile(File ksFile, String password) {

		// trying to read PEM-concatenated file first
		try {
			KeyStore pemks = KeyStore.getInstance(KeyStore.getDefaultType());
			pemks.load(null, null);
			PEMReader pr = new PEMReader(new FileReader(ksFile));
			Object obj = pr.readObject();
			int i = 0;
			while (obj != null) {
				if (obj instanceof java.security.cert.Certificate) {
					pemks.setCertificateEntry("cert" + i,
							(java.security.cert.Certificate) obj);
				}
				obj = pr.readObject();
				i++;
			}
			return pemks;
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}

		// trying to open going through all keystore format types supported
		Provider[] providers = Security.getProviders();
		Set<String> algorithms = new HashSet<String>(
				Security.getAlgorithms("KeyStore"));
		String defaultAlgo = KeyStore.getDefaultType();
		algorithms.remove(defaultAlgo);
		System.out.println(algorithms);

		for (String alg : algorithms) {
			for (Provider p : providers) {
				Service service = p.getService("KeyStore", alg);
				if (service != null) {
					System.out.println(service.getAlgorithm() + " "
							+ service.getProvider().getName());
				}
			}
		}

		FileInputStream fis = null;

		List<String> types = new ArrayList<String>(algorithms);
		types.add(0, defaultAlgo);

		for (String type : types) {
			try {
				fis = new FileInputStream(ksFile);
				KeyStore ks = openKeystoreFromStream(type, fis, password);
				if (ks != null) {
					System.out.println(type + " is used");
					return ks;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (KeyStoreException e) {
			} catch (NoSuchAlgorithmException e) {
			} catch (CertificateException e) {
			}
		}

		return null;
	}

}
