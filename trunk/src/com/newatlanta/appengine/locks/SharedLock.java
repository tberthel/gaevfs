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

import static com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService;

import com.google.appengine.api.memcache.InvalidValueException;

/**
 * <p>Implements a shared lock based on the Google App Engine <code>MemcacheService</code>,
 * specifically, the atomic <code>increment()</code> method. The lock is
 * acquired by incrementing the counter and released by decrementing it;
 * acquiring the lock never fails. The {@link #isLocked()} method can be
 * used to determine whether the lock has been acquired by any thread.
 * 
 * <p>There are three issues with the current implementation of this class
 * (the last is the most serious):
 * <ol>
 * <li>memcache is not reliable and the counter being used as a lock may be
 * evicted at any time, releasing the lock prematurely;</li>
 * 
 * <li>any thread can invoke {@link #unlock()} any number of times, 
 * regardless of whether that thread has ever acquired the lock, or how many
 * times it has acquired the lock--a "rogue" thread could therefore cause the
 * lock to be released prematurely; and,</li>
 *      
 * <li>there is no mechanism to insure that a lock is not held indefinitely
 * due to programming errors (failures to invoke {@link #unlock()}) or
 * abnormal JVM termination.</li>
 * </ol>
 * 
 * <p>The last issue could be addressed as follows if there was a way to delete
 * tasks from task queues:
 * <ul>
 * <li>When a lock is acquired (the counter incremented), queue a task to
 * release the lock (decrement the counter) at some time in the future (30
 * seconds--the maximum time a request can run?).</li>
 * 
 * <li>When the lock is released, delete the queued task so that it won't run.</li>
 * 
 * <li>If the lock is not released by the request thread, the queued task will
 * release it.</li>
 * </ul>
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class SharedLock extends AbstractLock {

    private String key;

    public SharedLock( String lockName ) {
        key = lockName;
    }

    public boolean tryLock() {
        try {
            Long counter = getMemcacheService().increment( key, 1, (long)0 );
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
            Long counter = getMemcacheService().increment( key, -1 );
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
            Long counter = (Long)getMemcacheService().get( key );
            return ( counter != null ? counter.longValue() : 0 );
        } catch ( Exception e ) {
            log.warning( e.toString() );
            return 0;
        }
    }

    /**
     * This method is guaranteed to never throw exceptions; the
     * <code>ReadWriteLock</code> class depends on this guarantee.
     */
    public boolean isLocked() {
        return ( getCounter() > 0 );
    }
}
