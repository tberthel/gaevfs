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
    private static final int EXPIRATION = 30; // seconds

    private String key;
    private Thread owner;
    private long holdCount;

    public ExclusiveLock( String lockName ) {
        key = lockName;
    }
    
    public String getName() {
        return key;
    }
    
    /**
     * Returns the thread that currently owns this lock; or, <code>null</code>
     * if not owned by a thread running within this JVM instance.
     */
    public Thread getOwner() {
        return owner;
    }
    
    public boolean isHeldByCurrentThread() {
        return ( Thread.currentThread() == owner );
   	}

    /**
     * Acquires the lock only if it is free at the time of invocation. For
     * re-entrant calls by owner, try to re-acquire the lock in case it was
     * evicted from memcache since originally acquired.
     */
    public synchronized boolean tryLock() {
        if ( ( owner != null ) && !isHeldByCurrentThread() ) {
            return false; // owned, but not by current thread
        }
        // unowned, or re-entrant lock by owner
        if ( !acquireLock() && !isHeldByCurrentThread() ) {
            return false;
        }
        owner = Thread.currentThread();
        holdCount++;
        log.info( "acquired " + key + " " + owner + " " + holdCount );
        return true;
    }
    
    protected boolean acquireLock() {
        // value of -1 causes MemcacheService.increment() to fail, which is what we want
        return memcache.put( key, (long)-1, Expiration.byDeltaSeconds( EXPIRATION ),
                                SetPolicy.ADD_ONLY_IF_NOT_PRESENT );
    }

    /**
     * Only the owner thread may unlock.
     * 
     * @throws IllegalStateException
     *         If an unlock attempt is made by a non-owner.
     */
    public synchronized void unlock() {
        if ( !isHeldByCurrentThread() ) {
            throw new IllegalStateException( "Attempted unlock by non-owner" );
        }
        if ( --holdCount == 0 ) {
            if ( !memcache.delete( key ) ) {
                log.warning( "not found: " + key );
            }
            owner = null;
        }
        log.info( "released " + key + " " + 
                    ( owner != null ? owner : Thread.currentThread() ) + " " +
                    holdCount );
    }
}
