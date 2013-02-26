package dan.serverchecks;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import dan.serverchecks.ServerChecks.ServerCheckCommand;


/**
 * Just to dump what java sees and takes from the system
 */
public class SystemProperties implements ServerCheckCommand{
	
	public static void dumpProps() {
		Properties props = System.getProperties();
		Set<Entry<Object, Object>> entrySet = props.entrySet();
		for (Entry<Object, Object> e : entrySet) {
			System.out.println(e.getKey() + ": " + e.getValue());
		}
	}
	
	public static void dumpEnv() {
		Map<String, String> props = System.getenv();
		Set<Entry<String, String>> entrySet = props.entrySet();
		for (Entry<String, String> e : entrySet) {
			System.out.println(e.getKey() + ": " + e.getValue());
		}
	}

	public void execute() {
		System.out.println("=Properties=");
		dumpProps();
		System.out.println();
		System.out.println("=Environ=");
		dumpEnv();
	}
	
}
