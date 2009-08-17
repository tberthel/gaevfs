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
        assertEquals( lockThread.hashCode(), lock.getOwnerHashCode() );
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
        assertEquals( 0, lock.getOwnerHashCode() );
        for ( int i = 0; i < 10; i++ ) {
            assertTrue( lock.tryLock() );
            assertEquals( Thread.currentThread().hashCode(), lock.getOwnerHashCode() );
        }
        for ( int i = 0; i < 9; i++ ) {
            lock.unlock();
            assertEquals( Thread.currentThread().hashCode(), lock.getOwnerHashCode() );
        }
        lock.unlock();
        assertEquals( 0, lock.getOwnerHashCode() );
    }

    @Test
    public void testLock() {
        Thread lockThread = LockingThread.createThread( lock, 2000 );
        assertEquals( lockThread.hashCode(), lock.getOwnerHashCode() );
        for ( int i = 0; i < 10; i++ ) {
            lock.lock();
            assertEquals( Thread.currentThread().hashCode(), lock.getOwnerHashCode() );
        }
        for ( int i = 0; i < 9; i++ ) {
            lock.unlock();
            assertEquals( Thread.currentThread().hashCode(), lock.getOwnerHashCode() );
        }
        lock.unlock();
        assertEquals( 0, lock.getOwnerHashCode() );
    }

     @Test
     public void testLockInterruptibly() {
         Thread lockThread = LockingThread.createThread( lock, 2000 );
         assertEquals( lockThread.hashCode(), lock.getOwnerHashCode() );
         try {
             for ( int i = 0; i < 10; i++ ) {
                 lock.lockInterruptibly();
                 assertEquals( Thread.currentThread().hashCode(), lock.getOwnerHashCode() );
             }
             for ( int i = 0; i < 9; i++ ) {
                 lock.unlock();
                 assertEquals( Thread.currentThread().hashCode(), lock.getOwnerHashCode() );
             }
             lock.unlock();
             assertEquals( 0, lock.getOwnerHashCode() );
        } catch ( InterruptedException e ) {
            fail( e.toString() );
        }
     }

     @Test
     public void testTryLockLongTimeUnit() {
         Thread lockThread = LockingThread.createThread( lock, Long.MAX_VALUE );
         assertEquals( lockThread.hashCode(), lock.getOwnerHashCode() );
         assertFalse( lock.tryLock( 200, TimeUnit.MILLISECONDS ) );
         lockThread.interrupt(); // release the lock
         try {
             Thread.sleep( 100 ); // give lockThread a chance to run
         } catch ( InterruptedException e ) {
         }
         assertEquals( 0, lock.getOwnerHashCode() );
         lockThread = LockingThread.createThread( lock, 1000 );
         assertEquals( lockThread.hashCode(), lock.getOwnerHashCode() );
         assertTrue( lock.tryLock( 2, TimeUnit.SECONDS ) );
         lock.unlock();
         assertEquals( 0, lock.getOwnerHashCode() );
     }
}