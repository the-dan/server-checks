package dan.serverchecks;

import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * Useful for daemon script debugging, TERM/KILL signals
 * handling
 * Upstart, cron scripts, systemd, commons-daemon, etc. 
 */
public class SleepyDaemon implements ServerCheckCommand {
	
	@Parameter(names = {"-s", "--sleep"}, description="Be active for, secs. Use 0 for infinity. Default: 60 secs")
	public long sleepTime = -1;
	
	public void execute() {
		if (sleepTime == -1) {
			sleepTime = 60;
		} else if (sleepTime == 0) {
			sleepTime = Long.MAX_VALUE;
		}
		
		if (sleepTime == Long.MAX_VALUE) {
			System.out.println("Sleeping forever");
		} else {
			System.out.println("Sleeping for " + sleepTime + " secs");
		}
		
		Thread t = new Thread(new Runnable() {

			public void run() {
				System.out.println("Shutdown hook executed. Assuming normal TERMination, not KILLed");
			}
			
		});
		
		Runtime.getRuntime().addShutdownHook(t);
		
		try {
			Thread.sleep(TimeUnit.MILLISECONDS.convert(sleepTime, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			System.err.println("Who's woke me up?");
			e.printStackTrace();
		}
		
	}
	
}
