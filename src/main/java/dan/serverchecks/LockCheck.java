package dan.serverchecks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.beust.jcommander.Parameter;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * Similar to DeadlockCheck, but there's not deadlock involved. Just to show how
 * different types of locks are seen in stack traces.
 * 
 * Implements following types of blocking scenarios:
 * <ul>
 * <li>wait for synchronized object
 * <li>wait for reentrant lock
 * <li>wait for RW lock for reading
 * <li>wait for socket read
 * <li>joining thread
 * </ul>
 * Object.wait() waiting
 */
public class LockCheck implements ServerCheckCommand {

	public static enum CustomerType {
		JOINING(new JoiningCustomer()),
		LOCKING(new LockingCustomer()),
		WAITING(new ObjectWaitingCustomer()),
		SYNCING(new SyncSleepWait()),
		RWLOCKING(new RWLockingCustomer())
		;
		
		Customer c;
		
		CustomerType(Customer c) {
			this.c = c;
		}
	}
	
	@Parameter(names = {"-t", "--lock-type"}, required = false)
	public CustomerType lockType = CustomerType.SYNCING;
	
	@Parameter(names = { "-w", "--wait" }, required=false, description="Wait on lock for, secs")
	public int waitInSecs = 60; 
			
	public static abstract class  Customer {
		protected long waitForMs;
		
		public void setWaitTimeInMs(long waitForMs) {
			this.waitForMs = waitForMs;
		}
		
		// public contract with waiter
		public abstract void makeOrder();

		// internal affairs of customer
		public abstract void choose();
	}

	public static class JoiningCustomer extends Customer {

		Thread wife;
		
		public void makeOrder() {
			try {
				wife.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Making order");
		}

		public void choose() {
			// delegating decision to wife
			wife = new Thread(new Runnable() {

				public void run() {
					try {
						Thread.sleep(waitForMs);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}, "wife");
			wife.start();
		}

	}

	public static class RWLockingCustomer extends Customer {

		ReadWriteLock lock = new ReentrantReadWriteLock();

		public void makeOrder() {
			Lock rl = lock.readLock();
			rl.lock();
			try {
				System.out.println("Making an order");
			} finally {
				rl.unlock();
			}
		}

		public void choose() {
			Lock wl = lock.writeLock();
			wl.lock();
			try {
				Thread.sleep(waitForMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				wl.unlock();
			}
		}

	}

	public static class ObjectWaitingCustomer extends Customer {

		Object lockObj = new Object();

		public void makeOrder() {
			synchronized (lockObj) {
				try {
					lockObj.wait(waitForMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Making an order");
		}

		public void choose() {
			synchronized (lockObj) {
				try {
					lockObj.wait(waitForMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static class SyncSleepWait extends Customer {

		public void makeOrder() {
			synchronized (this) {
				System.out.println("Making an order");
			}
		}

		public void choose() {
			synchronized (this) {
				try {
					Thread.sleep(waitForMs);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static class LockingCustomer extends Customer {
		Lock lock = new ReentrantLock();

		public void choose() {
			lock.lock();
			try {
				Thread.sleep(waitForMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
			}
		}

		public void makeOrder() {
			lock.lock();
			try {
				System.out.println("Making an order");
			} finally {
				lock.unlock();
			}
		}
	}

	public static class Waiter {
		public void takeOrder(Customer c) {
			c.makeOrder();
		}
	}

	public static class CustomerThread implements Runnable {
		private Customer c;

		public CustomerThread(Customer c) {
			this.c = c;
		}

		public void run() {
			System.out.println("Looking at the menu");
			c.choose();
		}
	}

	public static class WaiterThread implements Runnable {

		private Waiter w;
		private Customer c;

		public WaiterThread(Waiter w, Customer c) {
			this.w = w;
			this.c = c;
		}

		public void run() {
			System.out.println("Taking order");
			w.takeOrder(c);
		}

	}

	public void execute() {
		Customer c = this.lockType.c;
		System.out.println("Using " + this.lockType.name());
		
		long waitInMs = TimeUnit.SECONDS.toMillis(waitInSecs);
		c.setWaitTimeInMs(waitInMs);
		
		Waiter w = new Waiter();

		Thread tt = new Thread(new WaiterThread(w, c), "waiter-thread");
		Thread ct = new Thread(new CustomerThread(c), "customer-thread");

		// TODO: is there any guarantee customer will run before waiter?
		ct.start();
		tt.start();

	}

}
