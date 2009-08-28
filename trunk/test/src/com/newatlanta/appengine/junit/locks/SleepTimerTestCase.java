package com.newatlanta.appengine.junit.locks;

import static org.junit.Assert.*;

import org.junit.Test;

import com.newatlanta.appengine.locks.AbstractLock;

public class SleepTimerTestCase {
    
    @Test
    public void testSleepTimer() {
        DummyLock lock = new DummyLock();
        for ( int i = 0; i < 8; i++ ) {
            assertEquals( 1 << i, lock.nextSleepTime() );
        }
        for ( int i = 0; i < 100; i++ ) {
            assertEquals( 128, lock.nextSleepTime() );
        }
    }

    private class DummyLock extends AbstractLock {

        private SleepTimer timer = new SleepTimer();
        
        public long nextSleepTime() {
            return timer.nextSleepTime();
        }
        
        public boolean tryLock() {
            return false;
        }

        public void unlock() {
        }  
    }
}
