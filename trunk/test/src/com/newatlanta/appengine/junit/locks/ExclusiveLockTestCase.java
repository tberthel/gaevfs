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

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.newatlanta.appengine.junit.LocalServiceTestCase;
import com.newatlanta.appengine.locks.ExclusiveLock;

/**
 * Tests <code>com.newatlanta.appengine.locks.ExclusiveLock</code>.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class ExclusiveLockTestCase extends LocalServiceTestCase {

    private ExclusiveLock lock;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        lock = new ExclusiveLock( "junit.exclusive.lock" );
    }

    @Test
    public void testTryLock() {
        Thread lockThread = LockingThread.createThread( lock, Long.MAX_VALUE );
        assertEquals( lockThread, lock.getOwner() );
        assertFalse( lock.isHeldByCurrentThread() );
        assertFalse( lock.tryLock() );
        try {
            lock.unlock();
            fail( "expected IllegalStateException: lock.unlock()" );
        } catch ( IllegalStateException e ) {
        }
        lockThread.interrupt(); // release the lock
        try {
            Thread.sleep( 100 ); // give lockThread a chance to run
        } catch ( InterruptedException e ) {
        }
        assertNull( lock.getOwner() );
        assertFalse( lock.isHeldByCurrentThread() );
        for ( int i = 0; i < 10; i++ ) {
            assertTrue( lock.tryLock() );
            assertEquals( Thread.currentThread(), lock.getOwner() );
            assertTrue( lock.isHeldByCurrentThread() );
        }
        for ( int i = 0; i < 9; i++ ) {
            lock.unlock();
            assertEquals( Thread.currentThread(), lock.getOwner() );
            assertTrue( lock.isHeldByCurrentThread() );
        }
        lock.unlock();
        assertNull( lock.getOwner() );
        assertFalse( lock.isHeldByCurrentThread() );
    }

    @Test
    public void testLock() {
        Thread lockThread = LockingThread.createThread( lock, 2000 );
        assertEquals( lockThread, lock.getOwner() );
        assertFalse( lock.isHeldByCurrentThread() );
        for ( int i = 0; i < 10; i++ ) {
            lock.lock();
            assertEquals( Thread.currentThread(), lock.getOwner() );
            assertTrue( lock.isHeldByCurrentThread() );
        }
        for ( int i = 0; i < 9; i++ ) {
            lock.unlock();
            assertEquals( Thread.currentThread(), lock.getOwner() );
            assertTrue( lock.isHeldByCurrentThread() );
        }
        lock.unlock();
        assertNull( lock.getOwner() );
        assertFalse( lock.isHeldByCurrentThread() );
    }

     @Test
     public void testLockInterruptibly() {
         Thread lockThread = LockingThread.createThread( lock, 2000 );
         assertEquals( lockThread, lock.getOwner() );
         assertFalse( lock.isHeldByCurrentThread() );
         try {
             for ( int i = 0; i < 10; i++ ) {
                 lock.lockInterruptibly();
                 assertEquals( Thread.currentThread(), lock.getOwner() );
                 assertTrue( lock.isHeldByCurrentThread() );
             }
             for ( int i = 0; i < 9; i++ ) {
                 lock.unlock();
                 assertEquals( Thread.currentThread(), lock.getOwner() );
                 assertTrue( lock.isHeldByCurrentThread() );
             }
             lock.unlock();
             assertNull( lock.getOwner() );
             assertFalse( lock.isHeldByCurrentThread() );
        } catch ( InterruptedException e ) {
            fail( e.toString() );
        }
     }

     @Test
     public void testTryLockLongTimeUnit() throws InterruptedException {
         Thread lockThread = LockingThread.createThread( lock, Long.MAX_VALUE );
         assertEquals( lockThread, lock.getOwner() );
         assertFalse( lock.isHeldByCurrentThread() );
         assertFalse( lock.tryLock( 200, TimeUnit.MILLISECONDS ) );
         lockThread.interrupt(); // release the lock
         Thread.sleep( 100 ); // give lockThread a chance to run
         assertNull( lock.getOwner() );
         assertFalse( lock.isHeldByCurrentThread() );
         lockThread = LockingThread.createThread( lock, 1000 );
         assertEquals( lockThread, lock.getOwner() );
         assertFalse( lock.isHeldByCurrentThread() );
         assertTrue( lock.tryLock( 2, TimeUnit.SECONDS ) );
         assertTrue( lock.isHeldByCurrentThread() );
         lock.unlock();
         assertNull( lock.getOwner() );
         assertFalse( lock.isHeldByCurrentThread() );
     }
}