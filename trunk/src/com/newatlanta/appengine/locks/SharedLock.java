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

import com.google.appengine.api.memcache.InvalidValueException;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;

/**
 * Implements a shared lock based on the GAE <code>MemcacheService</code> API,
 * specifically, the atomic <code>increment()</code> method. The lock is
 * "acquired" by incrementing the counter and "released" by decrementing it;
 * acquiring the lock never fails. The <code>isLock()</code> method can be
 * used to determine whether the lock has been acquired by any thread.
 * 
 * There are three potential issues with the current implementation of this class:
 * 
 *   1) memcache is not reliable and the counter being used as a lock may be
 * evicted at any time;
 * 
 *   2) any thread can invoke <code>unlock()</code> any number of times,
 * regardless of whether that thread has ever acquired the lock, or how many
 * times it has acquired the lock--a "rogue" thread could therefore cause the
 * lock to be released prematurely; and,
 *      
 *   3) there is no mechanism to insure that a lock is not held indefinitely
 * due to programming errors (failures to invoke <code>unlock()</code>).
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
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class SharedLock extends AbstractLock {

    private static MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    private String key;

    public SharedLock( String lockName ) {
        key = lockName;
        createCounter();
    }

    private void createCounter() {
        if ( memcache.put( key, (long)0, null, SetPolicy.ADD_ONLY_IF_NOT_PRESENT ) ) {
            log.info( "created " + key );
        }
    }

    public boolean tryLock() {
        try {
            Long counter;
            // null value means counter does not exist
            while ( ( counter = memcache.increment( key, 1 ) ) == null ) {
                // create then increment avoids race condition, but requires
                // three memcache calls to create a new counter
                createCounter();
            }
            log.info( "acquired " + key + " " + counter.longValue() );
            return true;
        } catch ( InvalidValueException e ) {
            log.warning( e.toString() );
            return false;
        }
    }

    public void unlock() {
        try {
            // MemcacheService guarantees to never decrement below 0
            Long counter = memcache.increment( key, -1 );
            log.info( "released " + key + " " + ( counter != null ? counter.longValue() : "-" ) );
        } catch ( InvalidValueException e ) {
            log.warning( e.toString() );
        }
    }

    /**
     * This method is guaranteed to never throw exceptions.
     */
    public long getCounter() {
        try {
            Long counter = (Long)memcache.get( key );
            if ( counter != null ) {
                return ( counter.longValue() );
            }
        } catch ( Exception e ) {
            log.warning( e.toString() );
        }
        return 0;
    }

    /**
     * This method is guaranteed to never throw exceptions; the
     * <code>ReadWriteLock</code> class depends on this guarantee.
     */
    public boolean isLocked() {
        return ( getCounter() > 0 );
    }
}
