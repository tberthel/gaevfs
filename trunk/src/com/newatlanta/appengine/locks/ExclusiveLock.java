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

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

/**
 * Implements an exclusive lock based on the GAE <code>MemcacheService</code> API,
 * specifically, the <code>put()</code> method specifying
 * <code>SetPolicy.ADD_ONLY_IF_NOT_PRESENT</code>. If the specified key can be
 * put successfully based on this policy, then the lock has been acquired. The
 * lock is released by deleting the key.
 * 
 * The GAE documentation says that requests timeout after "around 30 seconds,"
 * which has two implications:
 * 
 *   1) concerns about reliability of the <code>MemcacheService</code> are
 * minimized, since it seems unlikely that a value will be evicted within the
 * request processing limit; and,
 *      
 *   2) keys (locks) can be set to expire at an interval slightly longer than
 * the request timeout, insuring that a lock is not held indefinitely due to
 * programming errors.
 * 
 * This class does not support reentrant locks.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class ExclusiveLock extends AbstractLock {
	
	private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
	private static Expiration REQUEST_TIMEOUT = Expiration.byDeltaSeconds( 40 );
	
	private String key;
	
	public ExclusiveLock( String lockName ) {
		key = lockName;
	}
	
	/**
	 * Acquires the lock only if it is free at the time of invocation.
	 */
	public boolean tryLock() {
		return memcache.put( key, null, REQUEST_TIMEOUT, SetPolicy.ADD_ONLY_IF_NOT_PRESENT );
	}

	public void unlock() {
		memcache.delete( key );
	}
}
