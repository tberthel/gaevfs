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

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

/**
 * Implements a shared lock based on the GAE <code>MemcacheService</code> API,
 * specifically, the atomic <code>increment()</code> method. The lock is
 * "acquired" by increment the counter and "released" by decrementing it;
 * acquiring the lock never fails. The <code>isLock()</code> method can be
 * used to determine whether the lock has been acquired by any thread.
 * 
 * There are two issues with the current implementation of this class:
 * 
 *   1) memcache is not reliable and the counter being used as a lock may be
 * evicted at any time; and,
 *      
 *   2) there is no mechanism to insure that a lock is not held indefinitely
 * due to programming errors.
 * 
 * The reliability issue is probably best addressed by implemented persistent
 * counters backed by the GAE datastore; see the following for an example:
 * 
 *  http://blog.appenginefan.com/2009/04/efficient-global-counters-revisited.html
 *   
 * It might be best to wait until Task Queues are available for GAE/J so that
 * they can be used to implement a write-behind cache for persistent counters,
 * which should be more efficient that the implementation described in the
 * above reference.
 * 
 * This class does not support reentrant locks.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class SharedLock extends AbstractLock {
	
	private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
	
	private String key;
	
	public SharedLock( String lockName ) {
		key = lockName;
		initCounter(); // key must be created before increment() can be invoked
	}
	
	private void initCounter() {
		memcache.put( key, 0, null, SetPolicy.ADD_ONLY_IF_NOT_PRESENT );
	}

	public boolean tryLock() {
		memcache.increment( key, 1 );
		return true;
	}

	public void unlock() {
		memcache.increment( key, -1 );
	}
	
	/**
	 * This method is guaranteed to not throw any exceptions.
	 */
	public boolean isLocked() {
		try {
			Long lockValue = (Long)memcache.get( key );
			if ( lockValue == null ) {
				initCounter();
				return false;
			}
			return ( lockValue.longValue() > 0 );
		} catch ( Exception e ) {
			return false;
		}
	}
}
