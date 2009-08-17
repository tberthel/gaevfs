package com.newatlanta.appengine.junit.locks;

import org.junit.Test;

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Delegate;

import com.newatlanta.appengine.junit.LocalServiceTestCase;
import com.newatlanta.appengine.junit.TestEnvironment;
import com.newatlanta.appengine.locks.ExclusiveLock;

public class ExclusiveLockTestCase extends LocalServiceTestCase {

    private ExclusiveLock lock;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        lock = new ExclusiveLock( "junit.exclusive.lock" );
    }

    @Test
    public void testTryLock() {
        Thread lockThread = createLockThread( Long.MAX_VALUE );
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
        assertTrue( lock.tryLock() );
        assertEquals( Thread.currentThread().hashCode(), lock.getOwnerHashCode() );
        assertTrue( lock.tryLock() ); // re-entrant lock
        assertEquals( Thread.currentThread().hashCode(), lock.getOwnerHashCode() );
        lock.unlock();
        assertEquals( Thread.currentThread().hashCode(), lock.getOwnerHashCode() );
        lock.unlock();
        assertEquals( 0, lock.getOwnerHashCode() );
    }

    @Test
    public void testLock() {
        createLockThread( 500 );
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
         createLockThread( 500 );
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

    // @Test
    // public void testTryLockLongTimeUnit() {
    // fail( "Not yet implemented" );
    // }

    private Thread createLockThread( long sleepTime ) {
        Thread lockThread = new LockThread( ApiProxy.getDelegate(), sleepTime );
        lockThread.start();
        try {
            Thread.sleep( 100 ); // give lockThread a chance to run
        } catch ( InterruptedException e ) {
        }
        assertTrue( lockThread.isAlive() );
        assertEquals( lockThread.hashCode(), lock.getOwnerHashCode() );
        return lockThread;
    }

    /**
     * Acquires the lock via the lock() method, sleeps for the specified time or
     * until interrupted, then releases the lock.
     */
    private class LockThread extends Thread {

        @SuppressWarnings("unchecked")
        private Delegate delegate;
        private long sleepTime;

        @SuppressWarnings("unchecked")
        private LockThread( Delegate delegate, long sleepTime ) {
            super( "LockThread" );
            this.delegate = delegate;
            this.sleepTime = sleepTime;
        }

        public void run() {
            ApiProxy.setEnvironmentForCurrentThread( new TestEnvironment() );
            ApiProxy.setDelegate( delegate );

            lock.lock();
            try {
                sleep( sleepTime );
            } catch ( InterruptedException e ) {
            } finally {
                lock.unlock();
            }
        }
    }
}