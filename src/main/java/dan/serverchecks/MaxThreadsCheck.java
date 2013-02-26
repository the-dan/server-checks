package dan.serverchecks;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

public class MaxThreadsCheck implements ServerCheckCommand {
	
	@Parameter(names = {"-s", "--sleep"}, description = "How many seconds created thread will sleep for. Long.MAX_LONG if absent")
	public long threadSleepTime = Long.MAX_VALUE;
	
	@Parameter(names = {"-c", "--count"}, description = "Number of threads to create")
	public long count = 100;
	
	public static class SleepingThread extends Thread {
		private long sleepTime = 0;
		
		public SleepingThread(String name, long sleepTime) {
			super(name);
			this.sleepTime = sleepTime;
		}
		
		@Override
		public void run() {
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				System.out.println("Thread " + getName() + " interrupted");
				e.printStackTrace();
			}
		}
		
	}
	
	public void execute() {
		int i = 0;
		try {
			while (i < count) {
				SleepingThread t = new SleepingThread("sleep-" + i, threadSleepTime);
				t.start();
				i++;
			}
		} catch (Exception e) {
			System.err.println("Failed to start thread. Threads created: " + i);
			e.printStackTrace();
		} catch (Error e) {
			System.err.println("Failed to start thread. Error caught. Threads created: " + i);
			e.printStackTrace();
		}
	}
	
}
