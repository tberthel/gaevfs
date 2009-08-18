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

import org.junit.Before;
import org.junit.Test;

import com.newatlanta.appengine.junit.LocalServiceTestCase;
import com.newatlanta.appengine.locks.ReadWriteLock;

/**
 * Tests <code>com.newatlanta.appengine.locks.ReadWriteLock</code>.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class ReadWriteLockTestCase extends LocalServiceTestCase {

    private ReadWriteLock lock;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        lock = new ReadWriteLock( "junit.lock" );
    }

    @Test
    public void testReadLock() {
        Thread lockThread = LockingThread.createThread( lock.readLock(), Long.MAX_VALUE );
        assertFalse( "block writer", lock.writeLock().tryLock() );
        assertTrue( "multiple readers", lock.readLock().tryLock() );
        lock.readLock().unlock();
        lockThread.interrupt(); // release readLock
        try {
            Thread.sleep( 100 ); // give lockThread a chance to run
        } catch ( InterruptedException e ) {
        }
        assertTrue( "write lock (confirm unlock)", lock.writeLock().tryLock() );
        lock.writeLock().unlock();
        assertTrue( "read lock", lock.readLock().tryLock() );
        assertFalse( "block writer", lock.writeLock().tryLock() );
        assertTrue( "reentrant reader", lock.readLock().tryLock() );
    }

    @Test
    public void testWriteLock() {
        Thread lockThread = LockingThread.createThread( lock.writeLock(), Long.MAX_VALUE );
        assertFalse( "block writer", lock.writeLock().tryLock() );
        assertFalse( "block reader", lock.readLock().tryLock() );
        lockThread.interrupt(); // release writeLock
        try {
            Thread.sleep( 100 ); // give lockThread a chance to run
        } catch ( InterruptedException e ) {
        }
        assertTrue( "write lock (confirm unlock)", lock.writeLock().tryLock() );
        assertTrue( "reentrant writer", lock.writeLock().tryLock() );
        assertTrue( "reentrant reader", lock.readLock().tryLock() );
        lock.readLock().unlock();
    }
}
