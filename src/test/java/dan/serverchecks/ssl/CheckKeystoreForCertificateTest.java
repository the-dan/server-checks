package dan.serverchecks.ssl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;

public class CheckKeystoreForCertificateTest {

	static CryptoTestUtils.CertificateGenerator ca;
	static CryptoTestUtils.CertificateGenerator ica;
	static CryptoTestUtils.CertificateGenerator icaDifferentIssuer;
	static CryptoTestUtils.CertificateGenerator icaIssuerNameMatchesCA;

	@BeforeClass
	public static void createCertificateChain()
			throws NoSuchAlgorithmException, CertificateException,
			InvalidKeyException, SecurityException, SignatureException {
		// generate CA & private key
		// put CA cert into keystore
		Security.addProvider(new BouncyCastleProvider());
		
		
		ca = new CryptoTestUtils.CertificateGenerator();
		ca.generateKeys();
		ca.setIssuer("CA");
		ca.setSubject("CA");
		ca.setSigningKey(ca.privKey);
		ca.generateCert();
		
		// generate intermediate authority cert & key
		// put cert into another keystore
		ica = new CryptoTestUtils.CertificateGenerator();
		ica.generateKeys();
		ica.setIssuer(ca.subjectCN);
		ica.setSubject("ICA");
		ica.setSigningKey(ca.privKey);
		ica.generateCert();

		// generate another intermediate certificate authority with other issuer name
		// but same key
		// put it into another keystore
		icaDifferentIssuer = new CryptoTestUtils.CertificateGenerator();
		icaDifferentIssuer.setIssuer("UNKNOWN");
		icaDifferentIssuer.setSigningKey(ca.privKey);
		icaDifferentIssuer.setSubject("ICA-WITH-SAME-KEY-BUT-DIFFERENT-ISSUER");
		icaDifferentIssuer.pubKey = ica.pubKey;
		icaDifferentIssuer.generateCert();
		
		
		// generate intermediate certificate, that is signed not by ca
		// but matches ca's issuer name
		icaIssuerNameMatchesCA = new CryptoTestUtils.CertificateGenerator();
		icaIssuerNameMatchesCA.generateKeys();
		icaIssuerNameMatchesCA.setIssuer(ca.subjectCN);
		icaIssuerNameMatchesCA.setSigningKey(ica.privKey);
		icaIssuerNameMatchesCA.setSubject("ICA-MATCHIING-CA-ISSUER-NAME-BUT-NOT-SIGNED-WITH-IT");
		icaIssuerNameMatchesCA.generateCert();

		// generate leaf certificate signed by intermediate authority & key
		// it will be used for checks
		
	}

	@Test
	public void certificateUnderCheckMatchesCertificateInTrustore()
			throws FileNotFoundException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		CheckKeystoreForCertificate cmd = new CheckKeystoreForCertificate();
		cmd.isFile = true;

		cmd.truststoreFile = CryptoTestUtils.saveKeyStore(CryptoTestUtils.getKeyStoreWithCerts(ca.cert));
		cmd.params.add(CryptoTestUtils.saveCert(ca.cert));

		cmd.execute();
	}

	@Test
	public void certificateUnderCheckMatchesIssuerOfTrustedCertificateButPublicKeysDiffer() throws FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		CheckKeystoreForCertificate cmd = new CheckKeystoreForCertificate();
		cmd.isFile = true;
		cmd.truststoreFile = CryptoTestUtils.saveKeyStore(CryptoTestUtils.getKeyStoreWithCerts(ca.cert));
		cmd.params.add(CryptoTestUtils.saveCert(icaIssuerNameMatchesCA.cert));
		
		cmd.execute();
	}

	@Test
	public void publicKeyCertificateUnderCheckMatchesPublicKeyOfTrustedCertificate() throws FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		CheckKeystoreForCertificate cmd = new CheckKeystoreForCertificate();
		cmd.isFile = true;

		cmd.truststoreFile = CryptoTestUtils.saveKeyStore(CryptoTestUtils.getKeyStoreWithCerts(ica.cert));
		cmd.params.add(CryptoTestUtils.saveCert(icaDifferentIssuer.cert));

		cmd.execute();
	}

	@Test
	public void certificateUnderCheckFailsToBeCheckedAgainstEmptyTruststore() throws FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		CheckKeystoreForCertificate cmd = new CheckKeystoreForCertificate();
		cmd.isFile = true;

		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		File tmpFile = File.createTempFile("keystore", ".tmp");
		FileOutputStream fos = new FileOutputStream(tmpFile);
		ks.store(fos, "changeit".toCharArray());
		fos.close();
		String emptyKsPath = tmpFile.getAbsolutePath();
		
		cmd.truststoreFile = emptyKsPath;
		cmd.params.add(CryptoTestUtils.saveCert(icaDifferentIssuer.cert));

		cmd.execute();
	}

	public void certificateUnderCheckSubjectMatchesSubjectOfCertificateInTrustore() {
		
	}

}
