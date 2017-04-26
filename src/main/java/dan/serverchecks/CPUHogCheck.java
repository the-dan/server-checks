package dan.serverchecks;

import java.util.concurrent.TimeUnit;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

public class CPUHogCheck implements ServerCheckCommand {
	
	@Parameter(names = {"-p", "--pattern"}, description = "Pattern each thread executes. `r` is for run, `s` is for sleep")
	public String pattern = "rsrs";
	
	@Parameter(names = {"-c", "--count"}, description = "Number of threads to create")
	public long count = 100;
	
	@Parameter(names = {"-s", "--sleep"}, description = "How long sleep pattern phase must execute for, ms")
	public long phaseSleepDurationMs = 1000;
	
	@Parameter(names = {"-r", "--run"}, description = "Calibrate run iterations so that it will take about `run` milliseconds")
	public long phaseRunDurationMs = 1000;
	
	@Parameter(names = {"-m", "--max"}, description = "Stop thread execution after `max` iterations")
	public long stopAfterIterations = Long.MAX_VALUE;
	
	public static class PatternedThread extends Thread {
		private long max;
		private String pattern;
		private long runIterations = 10000;
		private long sleepDuration;
		private long runDuration;
		
		public PatternedThread(String name, String pattern, long sleepDuration, long runDuration, long max) {
			super(name);
			this.pattern = pattern;
			this.sleepDuration = sleepDuration;
			this.runDuration = runDuration;
			this.max = max;
		}
		
		@Override
		public void run() {
		    char[] phases = pattern.toCharArray();
		    
		    long iters = 0;
		    char phase = phases[0];
		    
		    while (iters < max) {
		        if (phase == 's') {
        			try {
        				Thread.sleep(sleepDuration);
        			} catch (InterruptedException e) {
        				System.out.println("Thread " + getName() + " interrupted");
        				e.printStackTrace();
        			}
		        } else if (phase == 'r') {
		            long start = System.nanoTime();
		            long c = 0;
		            for (int i = 0; i < runIterations; i++) {
		                c++;
		            }
		            long ranFor = System.nanoTime() - start;
		            System.out.println("Cycle took " + TimeUnit.NANOSECONDS.toMillis(ranFor) + " ms");
		            if (ranFor < TimeUnit.MILLISECONDS.toNanos(runDuration)) {
		                long nextRunIterations = (long) (runIterations * 1.0f / ranFor * TimeUnit.MILLISECONDS.toNanos(runDuration));
		                if (nextRunIterations <= 0) {
		                    System.out.println("Wrong calculations led to 0 iterations");
		                }
		                if (nextRunIterations < runIterations) {
		                    System.out.println("Running " + nextRunIterations + " next time. It's less than " + runIterations + ", though expected duration is greater than current one");
		                    runIterations = nextRunIterations;
		                }
		                runIterations = nextRunIterations;
		            }
		        } else {
		            // skip
		        }
		        
		        
		        
		        iters ++;
		        phase = phases[(int)(iters % phases.length)];
		    }
		}
		
	}
	
	public void execute() {
		int i = 0;
		try {
			while (i < count) {
			    PatternedThread t = new PatternedThread("thread-" + i, pattern, phaseSleepDurationMs, phaseRunDurationMs, stopAfterIterations);
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
