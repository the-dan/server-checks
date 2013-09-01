package dan.serverchecks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * Prints thread ID from Java POV and runs infinite loop in it. Useful for
 * figuring out how Java threads match OS thread. E.g. how Java's thread ID or
 * thread ID as seen in stack trace matches thread ID as seen in top/ps.
 * 
 */
public class BusyCheck implements ServerCheckCommand {

	@Parameter(required = false, names = { "-b", "--breath" }, description = "Breath for, ms. Without this you can stop the process only by killing it and the process will occupy whole 1 CPU core")
	public long breathSpaceMs = 10000;

	@Parameter(required = false, names = { "-s", "--spin" }, description = "Try to spin for, ms. Tries to make each spin occupy specified value of ms")
	public long expectedSpinDuration = 10000;

	@Parameter(required = false, names = { "-i", "--interactive" }, description = "Allows you to enter commands to stop busy thread or gather stats")
	public boolean isInteractive = false;
	
	@Parameter(required = false, names = {"-a", "--auto-stats" }, description = "Prints stats after each iteration")
	public boolean autoStats = false;

	public static class HardWorker implements Runnable {

		private long breathSpace;

		public HardWorker(long breathSpace, long expectedDuration) {
			this.breathSpace = breathSpace;
			this.expectedDuration = expectedDuration;
		}

		private BigInteger spinsSum = BigInteger.ZERO;
		private BigInteger spinsCount = BigInteger.ZERO;
		private long iterations = Integer.MAX_VALUE;
		private long expectedDuration;
		private long lastSpinDuration = 0;
		private AtomicBoolean isSpinning = new AtomicBoolean(false);
		private boolean autoStats = false;

		public BigDecimal getSpinAvgTimeInMs() {
			if (spinsCount.compareTo(BigInteger.ZERO) == 0) {
				return BigDecimal.ZERO;
			}
			return new BigDecimal(spinsSum).divide(new BigDecimal(spinsCount),
					RoundingMode.HALF_UP);
		}

		public long getIterations() {
			return iterations;
		}

		public boolean isSpinning() {
			return isSpinning.get();
		}

		public long getLaspSpinDurationInMs() {
			return TimeUnit.NANOSECONDS.toMillis(lastSpinDuration);
		}

		public void run() {

			for (;;) {
				long start = System.nanoTime();
				int c = 0;
				isSpinning.set(true);
				for (long i = 0; i < iterations; i++) {
					c++; // spin something
				}
				isSpinning.set(false);
				lastSpinDuration = System.nanoTime() - start;
				spinsSum = spinsSum.add(BigInteger.valueOf(TimeUnit.NANOSECONDS
						.toMillis(lastSpinDuration)));
				spinsCount = spinsCount.add(BigInteger.ONE);

				if (autoStats) {
					stats();
				}

				computeIterations(TimeUnit.NANOSECONDS
						.toMillis(lastSpinDuration));

				if (breathSpace != 0) {
					try {
						Thread.sleep(TimeUnit.MILLISECONDS
								.toMillis(breathSpace));
					} catch (InterruptedException e) {
						System.out
								.println("Hard worker has been interrupted. Breaking out of infinite loop");
						break;
					}
				}
			}
		}

		private void computeIterations(long spinDuration) {
			double opsPerMs = (double) iterations / (double) spinDuration;
			iterations = (long) (expectedDuration * opsPerMs);
		}

		public void stats() {
			BigDecimal ms = getSpinAvgTimeInMs();
			long s = TimeUnit.MILLISECONDS.toSeconds(ms.longValue());
			System.out.println("Avg spin duration, ms: " + ms + " (" + s
					+ ", secs)");
			long lsd = getLaspSpinDurationInMs();
			System.out.println("Last spin duration, ms: " + lsd + " ("
					+ TimeUnit.MILLISECONDS.toSeconds(lsd) + ", secs)");
			System.out.println("Iterations: " + getIterations());
			System.out.println("Status: "
					+ (isSpinning() ? "spinning" : "waiting"));
		}

		public void setAutostats(boolean autoStats) {
			this.autoStats = autoStats;
		}

	}

	public void execute() {
		HardWorker w = new HardWorker(breathSpaceMs, expectedSpinDuration);
		w.setAutostats(autoStats);
		Thread wt = new Thread(w, "hardworking-thread");
		System.out.println("Hard worker id is " + wt.getId());

		wt.start();

		if (!isInteractive) {

			try {
				wt.join();
			} catch (InterruptedException e) {
				System.err
						.println("Main thread interrupted while joinin hard worker");
			}
		} else {

			boolean as = false;
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			try {
				String line = br.readLine();
				while (line != null) {
					if (StringUtils.equalsIgnoreCase(line, "quit")
							|| StringUtils.equalsIgnoreCase(line, "q")) {
						wt.interrupt();
					} else if (StringUtils.equalsIgnoreCase(line, "stats")
							|| StringUtils.equalsIgnoreCase(line, "s")) {
						w.stats();
					} else if (StringUtils.equalsIgnoreCase(line, "as")
							|| StringUtils.equalsIgnoreCase(line, "autostats")) {
						as = !as;
						w.setAutostats(as);
					} else {
						System.out.println("Unknown command");
					}
					line = br.readLine();
				}
			} catch (IOException e) {
				System.err.println("What have you done? You broke me");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

}
