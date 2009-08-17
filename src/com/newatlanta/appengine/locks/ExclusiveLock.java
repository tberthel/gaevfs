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
 * This class supports reentrant locks.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class ExclusiveLock extends AbstractLock {

    private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    private static Expiration EXPIRATION = Expiration.byDeltaSeconds( 40 );

    private String key;
    private long lockCount;

    public ExclusiveLock( String lockName ) {
        key = lockName;
    }

    /**
     * Acquires the lock only if it is free at the time of invocation.
     */
    public boolean tryLock() {
        // thread id may not be unique across JVMs, so use hash code
        int hashCode = Thread.currentThread().hashCode();
        if ( !memcache.put( key, hashCode, EXPIRATION, SetPolicy.ADD_ONLY_IF_NOT_PRESENT )
                && ( hashCode != getOwnerHashCode() ) ) {
            return false;
        }
        lockCount++;
        return true;
    }

    /**
     * Only the owner thread may release the lock.
     */
    public void unlock() {
        int ownerHashCode = getOwnerHashCode();
        if ( ownerHashCode == 0 ) { // no owner
            return;
        }
        if ( ownerHashCode != Thread.currentThread().hashCode() ) {
            throw new IllegalStateException( "Attempt to unlock unowned lock" );
        }
        if ( --lockCount == 0 ) {
            memcache.delete( key );
        }
    }

    /**
     * Gets the owner of the lock.
     * 
     * @return the identifier of the thread that owns the lock, or 0 if there
     *         is no owner 
     */
    public int getOwnerHashCode() {
        Integer hashCode = (Integer)memcache.get( key );
        return ( hashCode != null ? hashCode.intValue() : 0 );
    }
}
