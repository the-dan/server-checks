package dan.serverchecks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class ServerChecks {
	
	static List<String> blackHole = new ArrayList<String>();
	
	public static interface ServerCheckCommand {
		public void execute();
	}
	
	public static class BlackHoleCommand implements ServerCheckCommand {
		@Parameter(names = { "-i", "--iterations" }, description = "Adds iterations count of \"0123456789\" into array")
		public long iterations = 1000000;
		@Parameter(names = { "-d", "--delay" }, description = "Delays N ms after each iteration")
		public long delay = 100;
		@Parameter(names = { "-f", "--file" }, description = "Read this file into memory")
		public String filename;
		
		/**
		 * Checks memory limits
		 */
		public void execute() {
			
			if (!StringUtils.isEmpty(filename)) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				File f = new File(filename);
				if (!f.exists()) {
					System.out.println("File not found " + filename);
					System.exit(1);
				}
				
				System.out.println("Reading file " + filename + " of " + f.length() + " bytes into memory");
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(f);
					while (true) {
						IOUtils.copyLarge(fis, baos, 0, 1024*1024*10);
						System.out.println("Read 10Mb of data");
					}
				} catch (IOException e) {
					IOUtils.closeQuietly(fis);
				}
			} else {
				
				System.out.println("Iterations: "
									+ iterations
									+ ", delay: "
									+ delay);
				
				long k = 0;
				try {
					for (k = 0; k < iterations; k++) {
						blackHole.add(new String("0123456789"));
						Thread.sleep(delay);
					}
				} catch (Throwable e) {
					System.out.println("Count: " + k);
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public static class DateCheck implements ServerCheckCommand {
		
		@Parameter(names = { "-d", "--date" }, description = "Date in dd.mm.yyyy format to get GMT offset at")
		public String offsetAtDate = null;
		
		/**
		 * Mainly for TZ offset checking,
		 * because Java uses it's own
		 * timezone database
		 */
		public void execute() {
			Date offsetDate = new Date();
			if (StringUtils.isEmpty(offsetAtDate)) {
				System.out.println("Using current date for GMT offset calculation");
			} else {
				SimpleDateFormat s = new SimpleDateFormat("dd.mm.yyyy");
				try {
					offsetDate = s.parse(offsetAtDate);
				} catch (ParseException e) {
					System.out.println("Wrong date format");
					System.exit(1);
				}
			}
			
			System.out.println("Date: " + new Date());
			System.out.println("Default timezone: " + TimeZone.getDefault());
			System.out.println("user.timezone property: " + System.getProperty("user.timezone"));
			TimeZone tz = TimeZone.getDefault();
			System.out.println("TZ display name: " + tz.getDisplayName());
			System.out.println("TZ DST savings: " + tz.getDSTSavings());
			System.out.println("TZ offset at the date specified, in hours " + TimeUnit.HOURS.convert(tz.getOffset(offsetDate.getTime()), TimeUnit.MILLISECONDS));
			
		}
		
	}
	
	public static class LocaleCheck implements ServerCheckCommand {

		/**
		 * Sometimes encoding used in Linux
		 * doesn't match encoding used in Java
		 * due to cumbersome rules from conversion
		 * from Linux to Java encoding
		 * 
		 * Plus useful to check if your script sets desired
		 * encoding
		 */
		public void execute() {
			Locale defLocale = Locale.getDefault();
			System.out.println("Country: " + defLocale.getCountry());
			System.out.println("Language: " + defLocale.getLanguage());
			System.out.println("Variant: " + defLocale.getVariant());
			System.out.println("ISO-3 Country: " + defLocale.getISO3Country());
			System.out.println("ISO-3 Language: " + defLocale.getISO3Language());
			System.out.println();
			
			String defaultFileEncoding = System.getProperty("file.encoding");
			System.out.println("File encoding (file.encoding property): " + defaultFileEncoding);
			
			String csn = Charset.defaultCharset().name();
			System.out.println("Default charset: " + csn);
		}
		
	}
	
	
	public void run(String [] args) {
		JCommander c = new JCommander(this);
		
		CommandsModule m = getCommandsModule();
		m.setCommander(c);
		
		m.configure();
		
		c.parse(args);
		
		String command = c.getParsedCommand();
		JCommander jc = c.getCommands().get(command);
		
		if (jc != null) {
			ServerCheckCommand scc = (ServerCheckCommand) jc
				.getObjects()
				.get(0);
			scc.execute();
		} else {
			Map<String, JCommander> commands = c.getCommands();
			for (Map.Entry<String, JCommander> uc : commands.entrySet()) {
				uc.getValue().usage();
			}
		}
	}

	private CommandsModule commandsModule;

	private CommandsModule getCommandsModule() {
		return this.commandsModule;
	}
	
	public void setCommandsModule(CommandsModule m) {
		this.commandsModule = m;
	}
	
	public static void main(String[] args) {
		ServerChecks s = new ServerChecks();
		s.setCommandsModule(new BasicCommandsModule());
		s.run(args);
	}
	
}
