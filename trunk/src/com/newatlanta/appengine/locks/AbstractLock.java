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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * An abstract implementation of the <code>java.util.concurrent.locks.Lock</code>
 * interface. Subclasses must implement the <code>tryLock()</code> and
 * <code>unlock()</code> methods. Subclasses will also need to implement the
 * <code>newCondition()</code> method if supported.
 * 
 * It may be possible for subclasses to provide more efficient implementations
 * of methods that invoke <code>tryLock()</code> within loops.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public abstract class AbstractLock implements Lock {

	protected static long MAX_SLEEP_TIME = 128; // milliseconds
	
	/**
	 * Acquires the lock. If the lock is not available then the current thread
	 * becomes disabled for thread scheduling purposes and lies dormant until
	 * the lock has been acquired.
	 * 
	 * Note that GAE request threads timeout after 30 seconds, so this won't
	 * run forever.
	 */
	public void lock() {
		long sleepTime = 1;
		while ( !tryLock() ) {
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
	public void lockInterruptibly() throws InterruptedException {
		long sleepTime = 1;
		while ( !tryLock() ) {
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
	public boolean tryLock( long time, TimeUnit unit ) {
		long waitTime = Math.min( 0, unit.toMillis( time ) );
		long startTime = System.currentTimeMillis();
		long sleepTime = 1;
		try {
			do {
				if ( tryLock() ) {
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
	 * Default implementation throws UnsupportedOperationException.
	 */
	public Condition newCondition() {
		throw new UnsupportedOperationException();
	}
}
