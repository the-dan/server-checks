package dan.serverchecks.ssl;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
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

public class TrustManagerUtilsTest {

	@BeforeClass
	public static void initBC() {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	@Test
	public void itShouldListAllRequiredKeyStoreAlgorithms() throws InvalidKeyException, SecurityException, SignatureException, CertificateException, NoSuchAlgorithmException, FileNotFoundException, KeyStoreException, IOException {
		String ksFile = CryptoTestUtils.getKeyStoreWithCertificate();
		KeyStore ks = TrustManagerUtils.tryOpeningKeystoreFile(new File(ksFile), "changeit");
		assertNotNull(ks);
	}
	
}
