package dan.serverchecks.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.IOUtils;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * This tool is useful when:
 * * you created truststore and/or keystore and want to check it out before changing production settings
 * * you want to debug connectivity to HTTPS endpoint: check whether server certificate is trusted 
 *
 */
public class CheckSSL implements ServerCheckCommand {

	@Parameter(description = "[<url>]", required = true)
	List<String> params = new ArrayList<String>();
	
	// TODO: implement
	@Parameter(names = {"-t", "--truststore"}, description = "Truststore to use")
	String truststoreFileName = "";
	
	// TODO: implement
	@Parameter(names = {"-k", "--keystore"}, description = "Keystore to use")
	String keystoreFileName = "";
	
	@Parameter(names = {"-s", "--save"}, description = "Save server certificates")
	boolean isSaveCertificates = false;

	public void execute() {
		try {

			if (params.size() < 1) {
				System.err.println("URL wasn't specified");
				return;
			}

			URLConnection conn = new URL(params.get(0)).openConnection();

			SavingTrustManager[] tms = SavingTrustManager
					.getWrappedDefaultTrustManagers();

			SSLContext context = SSLContext.getInstance("TLS");

			System.out
					.println("SSL context protocol: " + context.getProtocol());
			System.out.println("SSL context provider name: "
					+ context.getProvider().getName());

			context.init(null, tms, null);
			SSLSocketFactory ssf = context.getSocketFactory();

			if (conn instanceof HttpsURLConnection) {
				HttpsURLConnection scon = (HttpsURLConnection) conn;
				scon.setSSLSocketFactory(ssf);
			}

			// TODO: save certificates if some options is specified

			InputStream is = null;
			try {
				is = conn.getInputStream();
				IOUtils.copy(is, System.out);
			} catch (IOException e) {
				e.printStackTrace();
				IOUtils.closeQuietly(is);
			}

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
