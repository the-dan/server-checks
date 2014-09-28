package dan.serverchecks.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * How does SSL server certificate work?
 * * socket connection to the server established
 * * server sends SSL server certificates chain to the client in course of SSL handshake
 * * client tries to find a certificate path from server certificate (first in chain) to one of the root certificates
 *   in truststore
 * ** if client finds such a path, it moves on, SSL handshake is complete
 * ** if client doesn't find such a path, it terminates socket connection
 * 
 * This utility doesn't try to find a path, it simply performs 4 tests on certificate in returned chain:
 * * If one of the certificates' issuer matches one in trust store, it verifies whether chain certificate is signed by one in trust store.
 *   It is a strict check. If it passes it means there is a chance that chain is correct.
 * * If one of the chain certificates matches certificate in trust store directly. It is a strict check.
 *   In real-case scenarios it's weird. Because you can't use CA certificates in trust store on a server. But probably
 *   you are trying out some certificates to make sure they are in Java's keystore. Or you added server cert into your own
 *   trust store and try to check if everything's fine with it. 
 * * If one of the chain certificates matches certificate in trust store by public key. It doesn't mean anything from SSL standpoint, but
 *   it's interesting to see, because it means there's two certs with same key but different subjects.
 * * If one of the chain certificates matches certificate in trust store by subject. It doesn't mean anython from SSL standpoing, but it's
 *   interesting to see, because it means there's two certs with same subject but possibly different public keys. 
 *
 *
 *	Basically, this tool is useful when:
 *	* you connect to some server and receive PKIX path validation error. You can download server certificates using browser and try to
 *    check them manually, starting from the root. PKIX path validation error is not very informative, this way you can be sure.
 *  * you added a server certificates into a trust store and want to try it out. See also CheckSSL command for tests.
 *  
 *
 */
@Parameters(commandDescription = "Checks if provided certificates is in keystore. Provides additional troubleshooting info")
public class CheckKeystoreForCertificate implements ServerCheckCommand {

	@Parameter(required = false, names = { "-y", "--type" }, description = "Keystore type: JKS, PKCS12")
	String keystoreType = "JKS";

	@Parameter(required = false, names = { "-p", "--password" }, description = "Password to the keystore")
	char[] password = "changeit".toCharArray();

	@Parameter(required = false, names = { "-t", "--trust-store" }, description = "Truststore filename")
	String truststoreFile = "";

	@Parameter(required = false, names = { "-s", "--save" }, description = "Save fetched certificates to a file. Not used if -f option is specified")
	boolean saveCertificates = false;

	@Parameter(required = false, names = { "-f", "--cert-file" }, description = "Certificate file to check")
	boolean isFile = false;

	@Parameter(description = "[<filename>|<host:[port]>]")
	List<String> params = new ArrayList<String>();

	org.slf4j.Logger log = LoggerFactory.getLogger("keystore");

	public void execute() {
		try {

			if (params.size() < 1) {
				System.err.println("Filename nor host is specified");
				return;
			}

			String fileToCheck = params.get(0);

			Security.addProvider(new BouncyCastleProvider());

			ArrayList<X509Certificate> certs = new ArrayList<X509Certificate>();

			if (!isFile) {
				SavingTrustManager[] tms = SavingTrustManager
						.getWrappedDefaultTrustManagers();
				try {
					SSLContext context = SSLContext.getInstance("TLS");
					context.init(null, tms, null);
					SocketFactory ssf = context.getSocketFactory();

					String[] parts = fileToCheck.split(":");
					String host = parts[0];
					int port = 443;
					if (parts.length > 1) {
						port = NumberUtils.toInt(parts[1], 443);
					}

					Socket socket = ssf.createSocket(host, port);

					InputStream sis = null;
					OutputStream sos = null;
					try {
						sis = socket.getInputStream();
						sos = socket.getOutputStream();
						sos.write("GET / HTTP/1.0\r\n\r\n".getBytes());
						IOUtils.copy(sis, System.out);
					} catch (IOException e) {
						IOUtils.closeQuietly(sis);
						IOUtils.closeQuietly(sos);
						socket.close();
					}

				} catch (KeyManagementException e) {
					return;
				}

				for (SavingTrustManager tm : tms) {
					X509Certificate[] ss = tm.getServerCerts();
					if (ss != null) {
						certs.addAll(Arrays.asList(ss));
					}
				}

			} else {
				PEMReader pr = null;
				try {
					FileReader r = new FileReader(new File(fileToCheck));
					pr = new PEMReader(r);
					Object obj = pr.readObject();
					int i = 1;
					while (obj != null) {
						if (obj instanceof X509Certificate) {
							X509Certificate certToCheck = (X509Certificate) obj;
							certs.add(certToCheck);
							System.out.println("Object #" + i
									+ " in file is a certificate. Subject: "
									+ certToCheck.getSubjectX500Principal());
						} else {
							System.err
									.println("This is not a certificates. It is "
											+ obj.getClass());
							continue;
						}
						i++;
						obj = pr.readObject();
					}
				} catch (FileNotFoundException e) {
					System.err.println("File " + fileToCheck + " not found");
					return;
				} catch (IOException e) {
					System.err.println("Error reading " + fileToCheck + ": "
							+ e.getMessage());
					return;
				} finally {
					IOUtils.closeQuietly(pr);
				}
			}

			if (certs.size() < 1) {
				System.err
						.println("No certificates fetched for checking. Quitting");
				return;
			}

			KeyStore ks = openKeystoreFromFileOrDefault();
			if (ks == null) {
				return;
			}

			for (X509Certificate certToCheck : certs) {
				checkCertificate(certToCheck, ks);
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private KeyStore openKeystoreFromFileOrDefault() throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException {
		KeyStore ks = KeyStore.getInstance(keystoreType);

		if (StringUtils.isEmpty(truststoreFile)) {
			String javaHome = System.getProperty("java.home");
			truststoreFile = FilenameUtils.concat(javaHome,
					"lib/security/cacerts");
		}

		System.out.println("Using " + truststoreFile + " truststore");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File(truststoreFile));
			ks.load(fis, password);
		} catch (FileNotFoundException e) {
			System.err.println("File " + truststoreFile + " not found");
			return null;
		} catch (IOException e) {
			System.err.println("Error loading keystore: " + e.getMessage());
			return null;
		} finally {
			IOUtils.closeQuietly(fis);
		}

		return ks;
	}

	private void checkCertificate(X509Certificate certToCheck, KeyStore ks)
			throws KeyStoreException, FileNotFoundException, IOException,
			NoSuchAlgorithmException, CertificateException {
		System.out.println("=============================");
		System.out.println("Certificate under check info:");
		System.out.println("Subject: " + certToCheck.getSubjectX500Principal());
		System.out.println("Issuer: " + certToCheck.getIssuerX500Principal());
		System.out.println("SAN: " + certToCheck.getSubjectAlternativeNames());
		System.out.println("IAN: " + certToCheck.getIssuerAlternativeNames());

		PublicKey publicKeyToCheck = certToCheck.getPublicKey();

		String certToCheckAlias = ks.getCertificateAlias(certToCheck);
		if (certToCheckAlias != null) {
			System.out
					.println("Found certificate in truststore AS IS under alias "
							+ certToCheckAlias);
		}

		String publicKetMatch = null;
		String subjectMatch = null;
		String issuerMatch = null;

		Enumeration<String> aliases = ks.aliases();
		for (String alias : Collections.list(aliases)) {

			log.trace("Checking certificate under {} alias", alias);

			if (ks.isKeyEntry(alias)) {
				log.warn(alias + " is a key. It's strange");
				continue;
			}
			if (!ks.isKeyEntry(alias) && !ks.isCertificateEntry(alias)) {
				log.warn(alias + " is not a key nor certificates. What is it?");
				continue;
			}

			Certificate cert = ks.getCertificate(alias);
			log.trace("{} is a cert", alias);
			if (!(cert instanceof X509Certificate)) {
				log.warn(
						"{} is not a X509 certificates. It is {}. Can't check the type. Skipping",
						alias, cert.getType());
			}

			X509Certificate xcert = (X509Certificate) cert;
			PublicKey publicKey = xcert.getPublicKey();
			String algorithm = publicKey.getAlgorithm();

			log.trace("{} has {} public key type", alias, algorithm);

			if (publicKeyToCheck.equals(publicKey)) {
				publicKetMatch = alias;
			}

			if (certToCheck.getSubjectX500Principal().equals(
					xcert.getSubjectX500Principal())) {
				subjectMatch = alias;
			}

			if (certToCheck.getIssuerX500Principal().equals(
					xcert.getSubjectX500Principal())) {
				issuerMatch = alias;
			}

		}

		if (!StringUtils.isEmpty(publicKetMatch)) {
			System.out.println("PUBKEYS MATCHED " + publicKetMatch + " (IT DOESNT'T CONFIRM ANYTHING)");
		}
		if (!StringUtils.isEmpty(subjectMatch)) {
			System.out.println("SUBJECTS MATCHED " + subjectMatch
					+ " (IT DOESN'T CONFIRM ANYTHING)");
		}
		if (!StringUtils.isEmpty(issuerMatch)) {
			System.out.println("ISSUER MATCHED " + issuerMatch
					+ ". Verifying signature");
			try {
				X509Certificate issuerCertificate = (X509Certificate) ks
						.getCertificate(issuerMatch);
				certToCheck.verify(issuerCertificate.getPublicKey());
				System.out.println("Certificate under check is signed by "
						+ issuerMatch + " ("
						+ issuerCertificate.getSubjectX500Principal() + ")");
			} catch (Exception e) {
				System.err.println("Can't verify signature. " + e.getMessage());
			}

		}

	}

}
