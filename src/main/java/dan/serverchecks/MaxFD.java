package dan.serverchecks;
import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * Just to check whether Java sees limits
 * we've set 
 *
 */
public class MaxFD implements ServerCheckCommand {
	
	public void execute() {
		System.out.println("Arch: " + ManagementFactory.getOperatingSystemMXBean().getArch());
		System.out.println("Proc#: " + ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
		System.out.println("Name: " + ManagementFactory.getOperatingSystemMXBean().getName());
		System.out.println("LA: " + ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
		System.out.println("Version: " + ManagementFactory.getOperatingSystemMXBean().getVersion());
		
		try {
			ObjectName oname = new ObjectName("java.lang:type=OperatingSystem");
			AttributeList attrs = ManagementFactory.getPlatformMBeanServer().getAttributes(oname, new String[] { "MaxFileDescriptorCount", "OpenFileDescriptorCount" });
			
			for (Object attr : attrs) {
				Attribute a = (Attribute) attr;
				System.out.println(a.getName() + ": " + a.getValue());
			}
		} catch (MalformedObjectNameException e){
			e.printStackTrace();
		} catch (InstanceNotFoundException e) {
			e.printStackTrace();
		} catch (ReflectionException e) {
			e.printStackTrace();
		}
		
	}
	
}
