package dan.serverchecks.ssl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V3CertificateGenerator;

public class CryptoTestUtils {

	public static class CertificateGenerator {
		X509Certificate cert;
		PublicKey pubKey;
		PrivateKey privKey;
		private PrivateKey signingKey;
		private String issuerCN;
		String subjectCN;
		
		public void setSubject(String subjectCN) {
			this.subjectCN = subjectCN;
		}
		public void setIssuer(String issuerCN) {
			this.issuerCN = issuerCN;
		}
		public void setSigningKey(PrivateKey key) {
			this.signingKey = key;
		}
		
		public void generateKeys() throws NoSuchAlgorithmException {
			KeyPairGenerator kg = KeyPairGenerator.getInstance("RSA");
			kg.initialize(1024);
			KeyPair keyPair = kg.generateKeyPair();
			
			this.privKey = keyPair.getPrivate();
			this.pubKey = keyPair.getPublic();
		}
		
		public void generateCert() throws InvalidKeyException, SecurityException, SignatureException, CertificateException, NoSuchAlgorithmException {
			if (this.pubKey == null) {
				throw new IllegalStateException("Generate keys first. Public key required");
			}
			CertificateFactory cf = CertificateFactory.getInstance("X509");
			X509V3CertificateGenerator cgen = new X509V3CertificateGenerator();
	
			X500Principal subject = new X500Principal("CN=" + subjectCN);
			X500Principal issuer = new X500Principal("CN=" + issuerCN);
	
			Calendar c = Calendar.getInstance();
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			Date notBefore = c.getTime();
	
			c.add(Calendar.YEAR, 1);
			Date notAfter = c.getTime();
	
			cgen.setIssuerDN(issuer);
			cgen.setSubjectDN(subject);
			cgen.setNotAfter(notAfter);
			cgen.setNotBefore(notBefore);
			cgen.setPublicKey(this.pubKey);
	
			cgen.setSerialNumber(BigInteger.valueOf(1));
	
			cgen.setSignatureAlgorithm("SHA1WithRSAEncryption");
	
			X509Certificate cert = null;
			cert = cgen.generateX509Certificate(signingKey);
	
			this.cert = cert;
		
		}
	}

	public static KeyStore getKeyStoreWithCerts(X509Certificate... c)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setCertificateEntry("test", c[0]);
		return ks;
	}

	public static String saveKeyStore(KeyStore ks)
			throws FileNotFoundException, IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException {
		File tmpFile = File.createTempFile("keystore", ".tmp");
		FileOutputStream fos = new FileOutputStream(tmpFile);
		ks.store(fos, "changeit".toCharArray());
		fos.close();
		return tmpFile.getAbsolutePath();
	}

	public static String saveCert(X509Certificate c) throws IOException {
		File tmpFile = File.createTempFile("cert", ".tmp");
		PEMWriter pw = new PEMWriter(new FileWriter(tmpFile));
		pw.writeObject(c);
		pw.close();
		return tmpFile.getAbsolutePath();
	}
	
	public static String getKeyStoreWithCertificate() throws InvalidKeyException, SecurityException, SignatureException, CertificateException, NoSuchAlgorithmException, FileNotFoundException, KeyStoreException, IOException {
		CertificateGenerator g = new CertificateGenerator();
		g.generateKeys();
		g.setIssuer("CA");
		g.setSubject("CA");
		g.setSigningKey(g.privKey);
		g.generateCert();
		return saveKeyStore(getKeyStoreWithCerts(g.cert));
	}

}
