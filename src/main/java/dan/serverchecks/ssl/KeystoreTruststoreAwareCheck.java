package dan.serverchecks.ssl;

import java.io.File;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import org.apache.commons.lang.StringUtils;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

public class KeystoreTruststoreAwareCheck {

	private static final String DEFAULT_PASSWORD = "changeit";
	@Parameter(names = { "-t", "--truststore" }, description = "Truststore to use")
	String truststoreFileName = "";
	@Parameter(names = { "-tp", "--truststore-password" }, description = "Truststore password to use")
	String truststorePassword = "";
	@Parameter(names = { "-k", "--keystore" }, description = "Keystore to use")
	String keystoreFileName = "";
	@Parameter(names = { "-kp", "--keystore-password" }, description = "Keystore password")
	String keystorePassword = null;
	@Parameter(names = { "-sp", "--key-password" }, description = "Key encryption password")
	String keyPassword = DEFAULT_PASSWORD;

	public KeystoreTruststoreAwareCheck() {
		super();
	}

	public KeyManager[] keystoreFromCommandLineOrDefault()
			throws NoSuchAlgorithmException {
				KeyStore ks;
				
				if (!StringUtils.isEmpty(keystoreFileName)) {
					File ksFile = new File(keystoreFileName);
					if (!ksFile.exists()) {
						System.err.println("Keystore file " + keystoreFileName + " does not exist");
						return null;
					}
					
					if (StringUtils.isEmpty(keystorePassword)) {
						keystorePassword = DEFAULT_PASSWORD;
						System.out.println("Keystore password isn't specified. Using default one (i.e. changeit)");
					}
					
					ks = TrustManagerUtils.tryOpeningKeystoreFile(ksFile, keystorePassword);
					if (ks == null) {
						System.err.println("Failed to open keystore file " + ksFile);
						return null;
					}
					KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
					try {
						kmf.init(ks, keyPassword.toCharArray());
					} catch (UnrecoverableKeyException e) {
						e.printStackTrace();
					} catch (KeyStoreException e) {
						e.printStackTrace();
					}
					return kmf.getKeyManagers();
					
				} else {
					System.out.println("Using default keystore");
					return null;
				}
				
			}

	public SavingTrustManager[] trustStoreFromCommandLineOrDefault() {
		SavingTrustManager[] tms = SavingTrustManager
				.getWrappedDefaultTrustManagers();
		
		
		KeyStore ts = null;
		KeyStore ks = null;
	
		if (!StringUtils.isEmpty(truststoreFileName)) {
			File tsFile = new File(truststoreFileName);
			if (!tsFile.exists()) {
				System.err.println("Truststore file " + truststoreFileName
						+ "does not exist");
				return tms;
			}
	
			if (StringUtils.isEmpty(truststorePassword)) {
				truststorePassword = DEFAULT_PASSWORD;
				System.out
						.println("Truststore password isn't specified. Using default one (i.e. changeit)");
			}
	
			ts = TrustManagerUtils.tryOpeningKeystoreFile(tsFile,
					truststorePassword);
			if (ts == null) {
				System.err
						.println("Failed to open keystore file " + tsFile);
				return tms;
			}
			tms = SavingTrustManager.getWrappedTrustManagers(ts);
			
			System.out.println("Using trustore consisting of:");
			try {
				for (String alias : Collections.list(ts.aliases())) {
					if (!ts.isCertificateEntry(alias)) {
						continue;
					}
					Certificate cert = ts.getCertificate(alias);
					if (!StringUtils.equalsIgnoreCase(cert.getType(), "X.509")) {
						System.out.println("Certificate with alias " + alias + " is not a X509 certificate. It's type is " + cert.getType() + ". Unable to print info. Moving on");
						continue;
					}
					X509Certificate x509c = (X509Certificate) cert;
					System.out.println(alias + " " + x509c.getSubjectDN());
				}
				System.out.println();
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return tms;
	}

}