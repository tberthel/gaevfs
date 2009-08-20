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
 *   2) keys (locks) can be set to expire at the same interval as the request
 * timeout, insuring that a lock is not held indefinitely due to programming errors.
 * 
 * This class supports reentrant locks, but requires a matching unlock for every
 * lock in order to release the lock.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class ExclusiveLock extends AbstractLock {

    private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    private static Expiration EXPIRATION = Expiration.byDeltaSeconds( 30 );

    private String key;
    private long holdCount;

    public ExclusiveLock( String lockName ) {
        key = lockName;
    }
    
    public boolean isHeldByCurrentThread() {
        return isOwner( Thread.currentThread().hashCode() );
    }
    
    protected boolean isOwner( int hashCode ) {
    	return ( hashCode == getOwnerHashCode() );
   	}

    /**
     * Acquires the lock only if it is free at the time of invocation.
     */
    public boolean tryLock() {
    	int hashCode = Thread.currentThread().hashCode();
        if ( !acquireLock( hashCode ) && !isOwner( hashCode ) ) {
            return false; // put failed and lock not owned by this thread
        }
        // put succeeded or this thread already owns the lock
        holdCount++;
        return true;
    }
    
    protected boolean acquireLock( int hashCode ) {
        return memcache.put( key, hashCode, EXPIRATION, SetPolicy.ADD_ONLY_IF_NOT_PRESENT );
    }

    /**
     * Only the owner thread may unlock.
     * 
     * @throws IllegalStateException
     *         If an unlock attempt is made by a non-owner.
     */
    public void unlock() {
        if ( !isHeldByCurrentThread() ) {
            throw new IllegalStateException( "Attempted unlock by non-owner" );
        }
        if ( --holdCount == 0 ) {
            memcache.delete( key );
        }
    }

    /**
     * Gets the hash code of the owner of the lock.
     * 
     * @return the hash code of the thread that owns the lock, or 0 if there
     *         is no owner (the lock doesn't exist in memcache)
     */
    public int getOwnerHashCode() {
        Integer hashCode = (Integer)memcache.get( key );
        return ( hashCode != null ? hashCode.intValue() : 0 );
    }
}
