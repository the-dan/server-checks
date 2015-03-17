package dan.serverchecks;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openssl.PEMWriter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;
import dan.serverchecks.ssl.KeystoreTruststoreAwareCheck;
import dan.serverchecks.ssl.SavingTrustManager;

public class ServeSSL extends KeystoreTruststoreAwareCheck implements
		ServerCheckCommand {

	int port = 4433;

	public void execute() {

		try {
			SSLContext context = SSLContext.getInstance("TLS");

			System.out
					.println("SSL context protocol: " + context.getProtocol());
			System.out.println("SSL context provider name: "
					+ context.getProvider().getName());
			System.out.println();

			KeyManager[] kms = super.keystoreFromCommandLineOrDefault();
			SavingTrustManager[] tms = super
					.trustStoreFromCommandLineOrDefault();

			context.init(kms, tms, null);

			SSLServerSocketFactory ssf = context.getServerSocketFactory();
			String[] supportedCipherSuites = ssf.getSupportedCipherSuites();
			for (String s : supportedCipherSuites) {
				System.out.println(s);
			}

			SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(port);

			ss.setEnabledCipherSuites(ssf.getSupportedCipherSuites());
			ss.setNeedClientAuth(true);

			try {
				Socket s = ss.accept();

				OutputStream os = s.getOutputStream();
				os.write("PONG".getBytes());

				s.close();
			} catch (SSLHandshakeException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			X509Certificate[] clientCerts = tms[0].getClientCerts();
			if (clientCerts == null) {
				System.err.println("No client certificates captured");
				return;
			}
			for (int i = 0; i < clientCerts.length; i++) {
				X509Certificate c = clientCerts[i];
				String fn = "cert" + i + ".pem";
				System.out.println("Saving certificate "
						+ c.getSubjectX500Principal() + " to " + fn);
				PEMWriter w = null;
				try {
					w = new PEMWriter(new FileWriter(fn));
					w.writeObject(c);
				} finally {
					IOUtils.closeQuietly(w);
				}

			}

		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
