package dan.serverchecks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dan.serverchecks.ServerChecks.ServerCheckCommand;

/**
 * Similar to DeadlockCheck, but there's not deadlock involved. Just to show how
 * different types of locks are seen in stack traces.
 * 
 * Implements following types of blocking scenarios:
 * <ul>
 * <li>wait for synchonized object
 * <li>wait for reentrant lock
 * <li>wait for RW lock for reading
 * <li>wait for socket read
 * <li>joining thread
 * </ul>
 * Object.wait() waiting
 */
public class LockCheck implements ServerCheckCommand {

	public static interface Customer {
		// public contract with waiter
		public void makeOrder();

		// internal affairs of customer
		public void choose();
	}

	public static class JoiningCustomer implements Customer {

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
						Thread.sleep(60000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}, "wife");
			wife.start();
		}

	}

	public static class RWLockingCustomer implements Customer {

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
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				wl.unlock();
			}
		}

	}

	public static class ObjectWaitingCustomer implements Customer {

		Object lockObj = new Object();

		public void makeOrder() {
			synchronized (lockObj) {
				try {
					lockObj.wait(60000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Making an order");
		}

		public void choose() {
			synchronized (lockObj) {
				try {
					lockObj.wait(60000L);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static class SyncSleepWait implements Customer {

		public void makeOrder() {
			synchronized (this) {
				System.out.println("Making an order");
			}
		}

		public void choose() {
			synchronized (this) {
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static class LockingCustomer implements Customer {
		Lock lock = new ReentrantLock();

		public void choose() {
			lock.lock();
			try {
				Thread.sleep(60000);
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
		// Customer c = new SyncSleepWait();
		// Customer c = new LockingCustomer();
		// Customer c = new ObjectWaitingCustomer();
		// Customer c = new RWLockingCustomer();
		Customer c = new JoiningCustomer();
		Waiter w = new Waiter();

		Thread tt = new Thread(new WaiterThread(w, c), "waiter-thread");
		Thread ct = new Thread(new CustomerThread(c), "customer-thread");

		// TODO: is there any guarantee customer will run before waiter?
		ct.start();
		tt.start();

	}

}
