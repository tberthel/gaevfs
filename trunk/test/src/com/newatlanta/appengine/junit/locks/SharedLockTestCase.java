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