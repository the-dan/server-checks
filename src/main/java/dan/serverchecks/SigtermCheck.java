package dan.serverchecks;

import sun.misc.Signal;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * 
 * Allows you to figure out how JVM behaves regarding different signals.
 * 
 * Possible tests: 1. Running program under Windows or Linux. Pressing Ctrl+C
 * should gracefully stop process (calling shutdown hook first). Threads waiting
 * for something are not interrupted (they are killed silently). 2. Running
 * program under Windows or Linux. Killing process from TaskManager or sending
 * SIGKILL should silently kill process, no shutdown hook should be called. 3.
 * Calling System.exit(n) from within a program should call shutdown hook first,
 * but threads, that are waiting for something (sleeping or blocked) or running
 * (like infinite loop or really doing some computation), are not interrupted
 * (they are killed silently).
 * 
 */
@SuppressWarnings("restriction")
public class SigtermCheck implements ServerCheckCommand {

	@Parameter(names = { "-s", "--signal" }, arity = 1, description = "Registers signal handler for specified signal number")
	int sigNum = 0;

	public static class SleepingRunnable implements Runnable {
		private long sleep = Long.MAX_VALUE;

		public SleepingRunnable() {
		}

		public SleepingRunnable(long sleep) {
			this.sleep = sleep;
		}

		public void run() {
			try {
				Thread.sleep(this.sleep);
			} catch (InterruptedException e) {
				System.out.println("Thread " + Thread.currentThread().getName()
						+ " has been interrupted");
			}
		}
	}

	public void execute() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("Shutdown hook executed");
			}
		});

		Thread sleeper = new Thread(new SleepingRunnable(), "sleeper");
		sleeper.start();

		if (sigNum != 0) {
			// TODO: Looks like sigNum should be a string, so we should replace TERM with sigNum
			sun.misc.Signal signal = new sun.misc.Signal("TERM");
			sun.misc.Signal.handle(signal, new sun.misc.SignalHandler() {

				public void handle(Signal sig) {
					int number = sig.getNumber();
					System.out.println("Signal " + number + " raised");
				}

			});
		}

		System.out.println("Sigterm is running. Press Ctrl+C to exit");

		try {
			Thread.sleep(Long.MAX_VALUE);
		} catch (InterruptedException e) {
			System.out.println("I've been interrupted!");
		}
	}

}
