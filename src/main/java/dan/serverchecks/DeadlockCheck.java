package dan.serverchecks;

import java.util.concurrent.locks.ReentrantLock;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * 
 * Simulates different kinds of deadlocks: * synchronized deadlock *
 * ReentrantLock deadlock * Object.wait() deadlock
 * 
 * They are shown a little bit different on stacktraces, so we make it an option
 * to choose one.
 * 
 * As a matter of fact, ReentrantLock and synchronized deadlocks are show in
 * stacktrace, but wait() deadlocks are not
 * 
 */
public class DeadlockCheck implements ServerCheckCommand {

	@Parameter(names = { "-l", "--lock" }, description = "Use ReentrantLock instead of synchronized")
	public boolean isLock = false;

	@Parameter(names = { "-o", "-w" }, description = "Use Object.wait() lock")
	public boolean isObjectLock = false;

	private static enum LockType {
		SYNCHRONIZED, LOCK, OBJECT
	}

	public static class Deadlocker implements Runnable {

		Deadlocker peer;
		private LockType lockType;

		public void setPeer(Deadlocker peer) {
			this.peer = peer;
		}

		public Deadlocker(LockType t) {
			this.lockType = t;
		}

		private ReentrantLock lock = new ReentrantLock();
		private Object obj = new Object();

		public void run() {

			for (;;) {
				switch (lockType) {
				case LOCK:
					this.lockAction();
					break;
				case OBJECT:
					this.objAction();
					break;
				case SYNCHRONIZED:
					this.syncAction();
					break;
				}

			}
		}

		private void objAction() {
			synchronized (peer.obj) {
				try {
					peer.obj.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			String tn = Thread.currentThread().getName();
			System.out.println(tn + " action");
			peer.objAction();
			synchronized (obj) {
					peer.obj.notify();
			}
		}

		public void lockAction() {
			lock.lock();
			try {
				String tn = Thread.currentThread().getName();
				System.out.println(tn + " action");
				peer.lockAction();
			} finally {
				lock.unlock();
			}
		}

		public synchronized void syncAction() {
			String tn = Thread.currentThread().getName();
			System.out.println(tn + " action");
			peer.syncAction();
		}

	}

	public void execute() {
		
		LockType type = LockType.SYNCHRONIZED;
		if (isLock && isObjectLock) {
			System.err.println("You can't use both object and ReentrantLock");
			System.exit(1);
		}
		
		if (isLock) {
			type = LockType.LOCK;
		}
		if (isObjectLock) {
			type = LockType.OBJECT;
		}
		
		
		Deadlocker a = new Deadlocker(type);
		Deadlocker b = new Deadlocker(type);
		a.setPeer(b);
		b.setPeer(a);

		Thread at = new Thread(a, "a");
		Thread bt = new Thread(b, "b");

		at.start();
		bt.start();

	}

}
