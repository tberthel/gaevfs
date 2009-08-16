/*
 * Copyright 2009 New Atlanta Communications, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.newatlanta.appengine.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Implements a "many readers, one writer" scheme. The write lock can be acquired
 * only if there are no readers and no writer already owning the lock. The read
 * lock can be acquired only if there is no writer that already owns the lock.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class ReadWriteLock implements java.util.concurrent.locks.ReadWriteLock {

	private ReadLock readLock;
	private WriteLock writeLock;
	
	public ReadWriteLock( String lockName ) {
		readLock = new ReadLock( lockName + ".readLock" );
		writeLock = new WriteLock( lockName + ".writeLock" );
	}
	
	public Lock readLock() {
		return readLock;
	}

	public Lock writeLock() {
		return writeLock;
	}
	
	/**
	 * Implements a SharedLock with an associated exclusive write lock.
	 */
	private class ReadLock extends SharedLock {

		private ReadLock( String lockName ) {
			super( lockName );
		}
		
		/**
		 * Acquires a shared read lock. First acquires the exclusive write lock
		 * to make sure no other thread has it, then acquires the read lock (which
		 * never fails). Releases the write lock before returning.
		 * 
		 * @return <code>true</code> if the read lock was acquired successfully
		 *         <code>false</code> if unable to acquire the read lock due to
		 *         the inability to acquire the associated write lock
		 */
		@Override
		public boolean tryLock() {
			if ( writeLock.tryLock() ) {
				try {
					return super.tryLock();
				} finally {
					writeLock.unlock();
				}
			}
			return false; // couldn't acquire writeLock
		}
	}
	
	/**
	 * Implements an ExclusiveLock with an associated shared read lock.
	 */
	private class WriteLock extends ExclusiveLock {

		private WriteLock( String lockName ) {
			super( lockName );
		}
		
		/**
		 * Acquires the lock. If the lock is not available then the current thread
		 * becomes disabled for thread scheduling purposes and lies dormant until
		 * the lock has been acquired.
		 * 
		 * Note that GAE request threads timeout after 30 seconds, so this won't
		 * run forever.
		 */
		@Override
		public void lock() {
			super.lock(); // acquire the exclusive lock
			long sleepTime = 1;
			while ( readLock.isLocked() ) { // make sure no readers
				try {
					// sleep twice as long after each iteration
					Thread.sleep( Math.min( MAX_SLEEP_TIME, sleepTime <<= 1 ) );
				} catch ( InterruptedException ignore ) {
				}
			}
		}
		
		/**
		 * Acquires the lock unless the current thread is interrupted.
		 * 
		 * Note that GAE request threads timeout after 30 seconds, so this won't
		 * run forever.
		 */
		@Override
		public void lockInterruptibly() throws InterruptedException {
			super.lockInterruptibly(); // acquire the exclusive lock
			long sleepTime = 1;
			while ( readLock.isLocked() ) { // make sure no readers
				// sleep twice as long after each iteration
				Thread.sleep( Math.min( MAX_SLEEP_TIME, sleepTime <<= 1 ) );
			}
		}
		
		/**
		 * Acquires the lock if it is free within the given waiting time and the
		 * current thread has not been interrupted.
		 * 
		 * Note that GAE request threads timeout after 30 seconds.
		 */
		@Override
		public boolean tryLock( long time, TimeUnit unit ) {
			long startTime = System.currentTimeMillis();
			super.tryLock( time, unit ); // acquire the exclusive lock
			long waitTime = Math.min( 0, unit.toMillis( time ) );
			long sleepTime = 1;
			try {
				do {
					if ( !readLock.isLocked() ) { // make sure no readers
						return true;
					}
					// sleep twice as long after each iteration
					Thread.sleep( Math.min( MAX_SLEEP_TIME, sleepTime <<= 1 ) );
				} while ( ( System.currentTimeMillis() - startTime ) < waitTime );
			} catch ( InterruptedException ignore ) {
			}
			return false;
		}
		
		/**
		 * Acquires an exclusive write lock. First acquires the write lock to make
		 * sure no other thread has it, then makes sure no readers already own the
		 * lock.
		 * 
		 * @return <code>true</code> if the write lock was acquired successfully
		 *         <code>false</code> if unable to acquire the write lock
		 */
		@Override
		public boolean tryLock() {
			if ( super.tryLock() ) { // acquire the exclusive write lock
				if ( !readLock.isLocked() ) { // make sure no readers
					return true;
				}
				super.unlock();
			}
			return false;
		}
	}
}