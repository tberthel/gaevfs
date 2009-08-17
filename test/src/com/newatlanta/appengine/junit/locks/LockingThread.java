package com.newatlanta.appengine.junit.locks;

import java.util.concurrent.locks.Lock;

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.newatlanta.appengine.junit.TestEnvironment;

public class LockingThread extends Thread {
    
    private Lock lock;
    private long sleepTime;
    
    @SuppressWarnings("unchecked")
    private Delegate delegate;
    
    public static Thread createThread( Lock lock, long sleepTime ) {
        Thread lockThread = new LockingThread( lock, sleepTime );
        lockThread.start();
        try {
            Thread.sleep( 200 ); // give lockThread a chance to run
        } catch ( InterruptedException e ) {
        }
        assert( lockThread.isAlive() );
        return lockThread;
    }
    
    private LockingThread( Lock lock, long sleepTime ) {
        super( "LockThread" );
        this.delegate = ApiProxy.getDelegate();
        this.sleepTime = sleepTime;
        this.lock = lock;
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