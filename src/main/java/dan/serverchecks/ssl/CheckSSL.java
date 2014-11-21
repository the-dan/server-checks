package dan.serverchecks.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * This tool is useful when: * you created truststore and/or keystore and want
 * to check it out before changing production settings * you want to debug
 * connectivity to HTTPS endpoint: check whether server certificate is trusted
 *
 */
public class CheckSSL implements ServerCheckCommand {

	private final static String DEFAULT_PASSWORD = "changeit";

	@Parameter(description = "[<url>]", required = true)
	List<String> params = new ArrayList<String>();

	// TODO: implement
	@Parameter(names = { "-t", "--truststore" }, description = "Truststore to use")
	String truststoreFileName = "";
	
	@Parameter(names = { "-tp", "--truststore-password" }, description = "Truststore password to use")
	String truststorePassword = "";

	// TODO: implement
	@Parameter(names = { "-k", "--keystore" }, description = "Keystore to use")
	String keystoreFileName = "";

	@Parameter(names = { "-kp", "--keystore-password" }, description = "Keystore password")
	String keystorePassword = null;
	
	@Parameter(names = {"-sp", "--key-password"}, description = "Key encryption password")
	String keyPassword = null;

	public void execute() {
		Security.addProvider(new BouncyCastleProvider());
		
		try {

			if (params.size() < 1) {
				System.err.println("URL wasn't specified");
				return;
			}

			URLConnection conn = new URL(params.get(0)).openConnection();

			SavingTrustManager[] tms = SavingTrustManager
					.getWrappedDefaultTrustManagers();
			KeyManager[] kms = null;
			
			KeyStore ts = null;
			KeyStore ks = null;

			if (!StringUtils.isEmpty(truststoreFileName)) {
				File tsFile = new File(truststoreFileName);
				if (!tsFile.exists()) {
					System.err.println("Truststore file " + truststoreFileName
							+ "does not exist");
					return;
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
					return;
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
			
			if (!StringUtils.isEmpty(keystoreFileName)) {
				File ksFile = new File(keystoreFileName);
				if (!ksFile.exists()) {
					System.err.println("Keystore file " + keystoreFileName + " does not exist");
					return;
				}
				
				if (StringUtils.isEmpty(keystorePassword)) {
					keystorePassword = DEFAULT_PASSWORD;
					System.out.println("Keystore password isn't specified. Using default one (i.e. changeit)");
				}
				
				ks = TrustManagerUtils.tryOpeningKeystoreFile(ksFile, keystorePassword);
				if (ks == null) {
					System.err.println("Failed to open keystore file " + ksFile);
					return;
				}
				KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				try {
					kmf.init(ks, keyPassword.toCharArray());
				} catch (UnrecoverableKeyException e) {
					e.printStackTrace();
				} catch (KeyStoreException e) {
					e.printStackTrace();
				}
				kms = kmf.getKeyManagers();
			}

			SSLContext context = SSLContext.getInstance("TLS");

			System.out
					.println("SSL context protocol: " + context.getProtocol());
			System.out.println("SSL context provider name: "
					+ context.getProvider().getName());
			System.out.println();

			context.init(kms, tms, null);
			SSLSocketFactory ssf = context.getSocketFactory();

			if (conn instanceof HttpsURLConnection) {
				HttpsURLConnection scon = (HttpsURLConnection) conn;
				scon.setSSLSocketFactory(ssf);
			}

			InputStream is = null;
			try {
				is = conn.getInputStream();
				IOUtils.copy(is, System.out);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(is);
			}

			System.out.println("Connection closed");

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}

}
