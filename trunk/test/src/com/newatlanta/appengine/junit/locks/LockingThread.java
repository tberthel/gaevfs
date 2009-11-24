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

import java.util.concurrent.locks.Lock;

/**
 * Acquires the specified lock, then sleeps for the specified time or until
 * interrupted.
 * 
 * @author <a href="mailto:vbonfanti@gmail.com">Vince Bonfanti</a>
 */
public class LockingThread extends Thread {
    
    private Lock lock;
    private long sleepTime;
    
    public static Thread createThread( Lock lock, long sleepTime ) {
        Thread lockThread = new LockingThread( lock, sleepTime );
        lockThread.start();
        try {
            Thread.sleep( 500 ); // give lockThread a chance to run
        } catch ( InterruptedException e ) {
        }
        assert( lockThread.isAlive() );
        return lockThread;
    }
    
    private LockingThread( Lock lock, long sleepTime ) {
        super( "LockThread" );
        this.sleepTime = sleepTime;
        this.lock = lock;
    }

    @Override
    public void run() {
        lock.lock();
        try {
            sleep( sleepTime );
        } catch ( InterruptedException e ) {
        } finally {
            lock.unlock();
        }
    }
}