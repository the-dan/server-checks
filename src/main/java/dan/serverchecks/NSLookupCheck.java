package dan.serverchecks;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;


public class NSLookupCheck implements ServerCheckCommand {
	
	@Parameter(names={"-a", "--address"}, description="Address to resolve")
	public String address;
	
	public static String dumpInetAddress(InetAddress ia) {
		StringBuilder b = new StringBuilder();
		b.append("Canonical hostname: ").append(ia.getCanonicalHostName()).append("\n");
		b.append("Host address: ").append(ia.getHostAddress()).append("\n");
		b.append("Hostname: ").append(ia.getHostName()).append("\n");
		b.append("Raw address: ").append(Arrays.toString(ia.getAddress())).append("\n");
		return b.toString();
	}

	public void execute() {
		try {
			System.out.println("=localhost=\n" + dumpInetAddress(InetAddress.getLocalHost()));
			if (!StringUtils.isEmpty(address)) {
				InetAddress resolved = InetAddress.getByName(address);
				System.out.println("=" + address + "=\n" + dumpInetAddress(resolved));
			}
		} catch (UnknownHostException e){
			System.out.println("Can't resolve");
			e.printStackTrace();
		}
	}
	
}
