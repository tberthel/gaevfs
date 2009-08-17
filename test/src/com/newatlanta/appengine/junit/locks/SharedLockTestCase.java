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
package com.newatlanta.appengine.junit.locks;

import org.junit.Test;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.newatlanta.appengine.junit.LocalServiceTestCase;
import com.newatlanta.appengine.locks.SharedLock;

/**
 * Tests <code>com.newatlanta.appengine.locks.SharedLock</code>.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class SharedLockTestCase extends LocalServiceTestCase {
    
    private static final String LOCK_NAME = "junit.shared.lock";
    
    private SharedLock lock;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        lock = new SharedLock( LOCK_NAME );
    }

    @Test
    public void testTryLock() {
        assertFalse( lock.isLocked() );
        Thread lockThread = LockingThread.createThread( lock, Long.MAX_VALUE );
        assertTrue( lock.isLocked() );
        for ( int i = 0; i < 20; i++ ) {
            assertTrue( lock.tryLock() );
            assertTrue( lock.isLocked() );
        }
        lockThread.interrupt(); // release the lock
        for ( int i = 0; i < 19; i++ ) {
            lock.unlock();
            assertTrue( lock.isLocked() );
        }
        lock.unlock();
        assertFalse( lock.isLocked() );
    }

    @Test
    public void testEvictLock() {
        assertFalse( lock.isLocked() );
        for ( int i = 0; i < 20; i++ ) {
            assertTrue( lock.tryLock() );
        }
        assertEquals( 20, lock.getCounter() );
        MemcacheServiceFactory.getMemcacheService().delete( LOCK_NAME );
        assertTrue( lock.tryLock() );
        assertEquals( 1, lock.getCounter() );
    }
}